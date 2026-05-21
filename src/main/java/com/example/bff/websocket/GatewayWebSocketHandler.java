package com.example.bff.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.*;

/**
 * WebSocket Proxy Handler for OrangePi Gateway devices.
 * Proxies WebSocket connections from BFF to WebSocket Service's /vx/gateway endpoint.
 * 
 * OrangePi connects to: ws://bff-server:8089/BFF/vx/gateway
 * BFF proxies to: ws://websocket-service:8082/vx/gateway
 */
@Component
public class GatewayWebSocketHandler implements WebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(GatewayWebSocketHandler.class);
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // OrangePi session ID -> OrangePi WebSocket session
    private final Map<String, WebSocketSession> gatewaySessions = new ConcurrentHashMap<>();
    
    // OrangePi session ID -> Backend WebSocket session  
    private final Map<String, WebSocketSession> backendSessions = new ConcurrentHashMap<>();
    
    // OrangePi session ID -> SafeNet Device ID (CPU ID)
    private final Map<String, String> sessionToDeviceId = new ConcurrentHashMap<>();
    
    // OrangePi session ID -> Client Type (gateway or device discovery)
    private final Map<String, String> sessionToClientType = new ConcurrentHashMap<>();
    
    // Locks for synchronizing concurrent sends on each session
    private final Map<String, Object> gatewaySessionLocks = new ConcurrentHashMap<>();
    private final Map<String, Object> backendSessionLocks = new ConcurrentHashMap<>();
    
    // Executor for async operations
    private final ExecutorService executor = Executors.newCachedThreadPool();

    @Value("${websocket.backend.gateway.url:ws://localhost:8082/vx/gateway}")
    private String backendGatewayUrl;

    @Value("${websocket.backend.deviceDiscovery.url:ws://localhost:8082/vx/gateway/deviceDiscovery}")
    private String backendDeviceDiscoveryUrl;

    @Value("${websocket.backend.deviceAssignment.url:ws://localhost:8082/vx/device-assignment}")
    private String backendDeviceAssignmentUrl;

    @Value("${websocket.backend.ebpfStats.url:ws://localhost:8082/vx/gateway/ebpfstats}")
    private String backendEbpfStatsUrl;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        String clientType = (String) session.getAttributes().get("clientType");
        
        gatewaySessions.put(sessionId, session);
        sessionToClientType.put(sessionId, clientType); // Store client type for reconnection
        // Initialize session locks to prevent concurrent sends
        gatewaySessionLocks.putIfAbsent(sessionId, new Object());
        backendSessionLocks.putIfAbsent(sessionId, new Object());

        logger.info("BFF Gateway WebSocket connection established - Session: {}, Client: {}, Address: {}", 
                sessionId, clientType, session.getRemoteAddress());

        // Immediately establish connection to backend
        connectToBackend(session, clientType);
    }

    /**
     * Establishes a WebSocket connection to the WebSocket Service's appropriate endpoint
     * based on client type (gateway or device discovery)
     */
    private void connectToBackend(WebSocketSession gatewaySession, String clientType) {
        String sessionId = gatewaySession.getId();
        String backendUrl = determineBackendUrl(clientType);
        
        logger.info("Connecting to backend URL: {} for client type: {}", backendUrl, clientType);
        
        executor.submit(() -> {
            try {
                StandardWebSocketClient client = new StandardWebSocketClient();
                
                WebSocketHandler backendHandler = new WebSocketHandler() {
                    @Override
                    public void afterConnectionEstablished(WebSocketSession backendSession) {
                        backendSessions.put(sessionId, backendSession);
                        logger.info("✓ Backend connection established for gateway session: {}", sessionId);
                        logger.info("  Backend session ID: {}", backendSession.getId());
                        logger.info("  Client Type: {}", clientType);
                    }

                    @Override
                    public void handleMessage(WebSocketSession backendSession, WebSocketMessage<?> message) {
                        // Proxy message from backend to OrangePi
                        proxyMessageToGateway(sessionId, message.getPayload().toString());
                    }

                    @Override
                    public void handleTransportError(WebSocketSession backendSession, Throwable exception) {
                        logger.error("Backend WebSocket transport error for gateway session {}: {}", 
                                sessionId, exception.getMessage());
                    }

                    @Override
                    public void afterConnectionClosed(WebSocketSession backendSession, CloseStatus closeStatus) {
                        logger.info("Backend WebSocket closed for gateway session {}: {}", sessionId, closeStatus);
                        backendSessions.remove(sessionId);
                        // Close OrangePi session if backend disconnects
                        closeGatewaySession(sessionId, closeStatus);
                    }

                    @Override
                    public boolean supportsPartialMessages() {
                        return false;
                    }
                };

                // Create WebSocketHttpHeaders to pass authentication
                org.springframework.web.socket.WebSocketHttpHeaders headers = new org.springframework.web.socket.WebSocketHttpHeaders();
                // Add any required auth headers if available from context
                // For now, WebSocket endpoints don't require auth; this is for future use
                
                CompletableFuture<WebSocketSession> future = client.execute(
                        backendHandler,
                        headers,
                        URI.create(backendUrl)
                );
                
                future.whenComplete((backendSession, throwable) -> {
                    if (throwable != null) {
                        logger.error("✗ Failed to connect to backend: {}", throwable.getMessage());
                        try {
                            sendError(gatewaySession, "BACKEND_CONNECTION_FAILED", 
                                    "Failed to connect to WebSocket Service");
                            gatewaySession.close(new CloseStatus(1011, "Backend connection failed"));
                        } catch (IOException e) {
                            logger.error("Error closing gateway session: {}", e.getMessage());
                        }
                        cleanup(sessionId);
                    } else {
                        logger.info("✓ Backend connection ready for session: {} (client: {})", sessionId, clientType);
                    }
                });

            } catch (Exception e) {
                logger.error("Error connecting to backend: {}", e.getMessage(), e);
            }
        });
    }

    /**
     * Determines the backend URL based on client type
     */
    private String determineBackendUrl(String clientType) {
        if ("orangepi_device_discovery".equals(clientType)) {
            return backendDeviceDiscoveryUrl;
        } else if ("orangepi_device_assignment".equals(clientType)) {
            return backendDeviceAssignmentUrl;
        } else if ("orangepi_ebpf_stats".equals(clientType)) {
            return backendEbpfStatsUrl;
        } else {
            return backendGatewayUrl;
        }
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        String sessionId = session.getId();
        String payload = message.getPayload().toString();
        
        logger.debug("Gateway message received - Session: {}, Payload length: {}", 
                sessionId, payload.length());

        // Try to extract device ID from message for logging/tracking
        extractDeviceId(sessionId, payload);

        // Proxy message to backend
        proxyMessageToBackend(sessionId, payload);
    }

    /**
     * Extract safenet device ID from incoming messages for session tracking
     */
    private void extractDeviceId(String sessionId, String payload) {
        try {
            JsonNode json = objectMapper.readTree(payload);
            
            // Check various possible fields for device ID
            String deviceId = null;
            if (json.has("safenetDeviceId")) {
                deviceId = json.get("safenetDeviceId").asText();
            } else if (json.has("safenet_device_id")) {
                deviceId = json.get("safenet_device_id").asText();
            } else if (json.has("device_cpu_id")) {
                deviceId = json.get("device_cpu_id").asText();
            } else if (json.has("gatewayId")) {
                deviceId = json.get("gatewayId").asText();
            } else if (json.has("cpuId")) {
                deviceId = json.get("cpuId").asText();
            }
            
            if (deviceId != null && !deviceId.isEmpty()) {
                String existingId = sessionToDeviceId.get(sessionId);
                if (existingId == null || !existingId.equals(deviceId)) {
                    sessionToDeviceId.put(sessionId, deviceId);
                    logger.info("Gateway device identified - Session: {}, Device ID: {}", sessionId, deviceId);
                }
            }
        } catch (Exception e) {
            // Ignore JSON parsing errors - not all messages may be JSON
            logger.trace("Could not parse message for device ID extraction: {}", e.getMessage());
        }
    }

    /**
     * Proxy message from OrangePi to backend
     */
    private void proxyMessageToBackend(String sessionId, String message) {
        WebSocketSession backendSession = backendSessions.get(sessionId);
        
        if (backendSession != null && backendSession.isOpen()) {
            try {
                // Synchronize on session lock to prevent concurrent sends
                Object lock = backendSessionLocks.get(sessionId);
                if (lock != null) {
                    synchronized (lock) {
                        backendSession.sendMessage(new TextMessage(message));
                        logger.debug("→ Proxied message to backend for session: {}", sessionId);
                    }
                }
            } catch (IOException e) {
                logger.error("Error proxying message to backend: {}", e.getMessage());
            }
        } else {
            logger.warn("No backend session available for gateway session: {}", sessionId);
            // Try to reconnect
            WebSocketSession gatewaySession = gatewaySessions.get(sessionId);
            String clientType = sessionToClientType.get(sessionId);
            if (gatewaySession != null && gatewaySession.isOpen() && clientType != null) {
                logger.info("Attempting to reconnect to backend for session: {} (client: {})", sessionId, clientType);
                connectToBackend(gatewaySession, clientType);
            }
        }
    }

    /**
     * Proxy message from backend to OrangePi
     */
    private void proxyMessageToGateway(String sessionId, String message) {
        WebSocketSession gatewaySession = gatewaySessions.get(sessionId);
        
        if (gatewaySession != null && gatewaySession.isOpen()) {
            try {
                // Synchronize on session lock to prevent concurrent sends
                Object lock = gatewaySessionLocks.get(sessionId);
                if (lock != null) {
                    synchronized (lock) {
                        gatewaySession.sendMessage(new TextMessage(message));
                        logger.debug("← Proxied message to gateway for session: {}", sessionId);
                    }
                }
            } catch (IOException e) {
                logger.error("Error proxying message to gateway: {}", e.getMessage());
            }
        } else {
            logger.warn("Gateway session not found or closed: {}", sessionId);
        }
    }

    /**
     * Send error message to OrangePi
     */
    private void sendError(WebSocketSession session, String code, String message) throws IOException {
        if (session.isOpen()) {
            String errorJson = objectMapper.writeValueAsString(Map.of(
                    "type", "error",
                    "code", code,
                    "message", message,
                    "timestamp", System.currentTimeMillis()
            ));
            // Synchronize on session lock to prevent concurrent sends
            Object lock = gatewaySessionLocks.get(session.getId());
            if (lock != null) {
                synchronized (lock) {
                    session.sendMessage(new TextMessage(errorJson));
                }
            }
        }
    }

    /**
     * Close gateway session
     */
    private void closeGatewaySession(String sessionId, CloseStatus status) {
        WebSocketSession gatewaySession = gatewaySessions.get(sessionId);
        if (gatewaySession != null && gatewaySession.isOpen()) {
            try {
                gatewaySession.close(status);
            } catch (IOException e) {
                logger.error("Error closing gateway session: {}", e.getMessage());
            }
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        String sessionId = session.getId();
        String deviceId = sessionToDeviceId.get(sessionId);
        
        logger.error("BFF Gateway WebSocket transport error - Session: {}, Device: {}, Error: {}", 
                sessionId, deviceId != null ? deviceId : "unknown", exception.getMessage());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) {
        String sessionId = session.getId();
        String deviceId = sessionToDeviceId.get(sessionId);
        
        logger.info("BFF Gateway WebSocket connection closed - Session: {}, Device: {}, Status: {}", 
                sessionId, deviceId != null ? deviceId : "unknown", closeStatus);
        
        // Close backend connection
        WebSocketSession backendSession = backendSessions.remove(sessionId);
        if (backendSession != null && backendSession.isOpen()) {
            try {
                backendSession.close(closeStatus);
            } catch (IOException e) {
                logger.error("Error closing backend session: {}", e.getMessage());
            }
        }
        
        cleanup(sessionId);
    }

    /**
     * Cleanup all session data
     */
    private void cleanup(String sessionId) {
        gatewaySessions.remove(sessionId);
        backendSessions.remove(sessionId);
        sessionToDeviceId.remove(sessionId);
        sessionToClientType.remove(sessionId);
        gatewaySessionLocks.remove(sessionId);
        backendSessionLocks.remove(sessionId);
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    /**
     * Get connected gateway count (for monitoring)
     */
    public int getConnectedGatewayCount() {
        return gatewaySessions.size();
    }

    /**
     * Get list of connected device IDs (for monitoring)
     */
    public Map<String, String> getConnectedDevices() {
        return Map.copyOf(sessionToDeviceId);
    }
}
