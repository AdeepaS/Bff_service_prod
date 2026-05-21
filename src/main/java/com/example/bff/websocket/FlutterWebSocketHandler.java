package com.example.bff.websocket;

import com.example.bff.config.RSAKeyRecord;
import com.example.bff.security.TokenDecryptionUtil;
import com.example.bff.utils.TokenBlacklist;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.*;

@Component
public class FlutterWebSocketHandler implements WebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(FlutterWebSocketHandler.class);
    private static final int AUTH_TIMEOUT_SECONDS = 10;

    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Flutter session -> Backend session mapping
    private final Map<String, WebSocketSession> flutterSessions = new ConcurrentHashMap<>();
    private final Map<String, WebSocketSession> backendSessions = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> authTimeoutTasks = new ConcurrentHashMap<>();
    private final Map<String, Integer> sessionUserIds = new ConcurrentHashMap<>();
    
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    @Autowired
    private RSAKeyRecord rsaKeyRecord;

    @Autowired
    private TokenBlacklist tokenBlacklist;

    @Value("${websocket.backend.url:ws://localhost:8082/vx/flutter}")
    private String backendWebSocketUrl;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        flutterSessions.put(sessionId, session);
        session.getAttributes().put("authenticated", false);

        logger.info("BFF Flutter WebSocket connection established - Session: {}, Address: {}, Auth timeout: {}s", 
                sessionId, session.getRemoteAddress(), AUTH_TIMEOUT_SECONDS);

        // Schedule auth timeout
        ScheduledFuture<?> timeoutTask = scheduler.schedule(() -> {
            handleAuthTimeout(sessionId);
        }, AUTH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        authTimeoutTasks.put(sessionId, timeoutTask);
    }

    @Override
    public void handleMessage(WebSocketSession session, org.springframework.web.socket.WebSocketMessage<?> message) throws Exception {
        String sessionId = session.getId();
        String payload = message.getPayload().toString();
        
        logger.info("=== BFF WebSocket Message Received ===");
        logger.info("Session: {}", sessionId);
        logger.info("Payload: {}", payload);

        Boolean isAuthenticated = (Boolean) session.getAttributes().get("authenticated");

        if (!Boolean.TRUE.equals(isAuthenticated)) {
            // Not authenticated yet - expect auth message
            handleAuthMessage(session, payload);
        } else {
            // Authenticated - proxy message to backend
            proxyMessageToBackend(sessionId, payload);
        }
    }

    private void handleAuthMessage(WebSocketSession session, String payload) throws IOException {
        String sessionId = session.getId();
        
        try {
            com.example.bff.DTO.WebSocketMessage authMessage = objectMapper.readValue(payload, com.example.bff.DTO.WebSocketMessage.class);

            if (!"auth".equals(authMessage.getType())) {
                sendErrorAndClose(session, "First message must be auth type", "INVALID_MESSAGE");
                return;
            }

            String token = authMessage.getToken();
            if (token == null || token.isEmpty()) {
                sendErrorAndClose(session, "Token is required", "MISSING_TOKEN");
                return;
            }

            // Validate JWT token (handles both encrypted and plain JWT)
            Integer userId = validateToken(token);
            if (userId == null) {
                sendErrorAndClose(session, "Invalid or expired token", "INVALID_TOKEN");
                return;
            }

            // Cancel auth timeout
            cancelAuthTimeout(sessionId);

            // Mark session as authenticated
            session.getAttributes().put("authenticated", true);
            session.getAttributes().put("userId", userId);
            sessionUserIds.put(sessionId, userId);

            logger.info("BFF Flutter authentication successful - Session: {}, User: {}", sessionId, userId);

            // Connect to backend WebSocket
            connectToBackend(session);

            // Send success response
            com.example.bff.DTO.WebSocketMessage successResponse = com.example.bff.DTO.WebSocketMessage.authSuccess(sessionId, userId);
            sendMessage(session, successResponse);

        } catch (Exception e) {
            logger.error("Auth message processing failed: {}", e.getMessage());
            sendErrorAndClose(session, "Authentication failed", "AUTH_FAILED");
        }
    }

    private Integer validateToken(String token) {
        try {
            String jwtToken = token;
            
            // Check if token is encrypted (not in JWT format)
            // JWT format has 3 parts separated by dots: header.payload.signature
            boolean isJwtFormat = token.contains(".") && token.split("\\.").length == 3;
            
            if (!isJwtFormat) {
                // Token is encrypted - decrypt it first
                logger.info("Token appears to be encrypted, decrypting...");
                try {
                    jwtToken = TokenDecryptionUtil.decryptToken(token);
                    logger.info("Token decrypted successfully");
                } catch (Exception e) {
                    logger.error("Token decryption failed: {}", e.getMessage());
                    return null;
                }
            } else {
                logger.info("Token is already in JWT format");
            }

            // Check if token is blacklisted (use original token for blacklist check)
            if (tokenBlacklist.isTokenBlacklisted(token)) {
                logger.warn("Token is blacklisted");
                return null;
            }

            // Decode and validate JWT
            JwtDecoder jwtDecoder = NimbusJwtDecoder.withPublicKey(rsaKeyRecord.rsaPublicKey()).build();
            Jwt jwt = jwtDecoder.decode(jwtToken);

            // Check expiration
            Instant expiration = jwt.getExpiresAt();
            if (expiration == null || expiration.isBefore(Instant.now())) {
                logger.warn("Token is expired");
                return null;
            }

            // Extract user ID from claims
            Object userIdClaim = jwt.getClaim("userId");
            if (userIdClaim == null) {
                // Try alternative claim names
                userIdClaim = jwt.getClaim("user_id");
                if (userIdClaim == null) {
                    userIdClaim = jwt.getClaim("sub");
                }
            }

            if (userIdClaim != null) {
                if (userIdClaim instanceof Integer) {
                    return (Integer) userIdClaim;
                } else if (userIdClaim instanceof Long) {
                    return ((Long) userIdClaim).intValue();
                } else if (userIdClaim instanceof String) {
                    return Integer.parseInt((String) userIdClaim);
                }
            }

            logger.warn("Could not extract userId from token");
            return null;

        } catch (JwtValidationException e) {
            logger.error("JWT validation failed: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            logger.error("Token validation error: {}", e.getMessage());
            return null;
        }
    }

    private void connectToBackend(WebSocketSession flutterSession) {
        String sessionId = flutterSession.getId();
        
        try {
            StandardWebSocketClient client = new StandardWebSocketClient();
            
            WebSocketHandler backendHandler = new WebSocketHandler() {
                @Override
                public void afterConnectionEstablished(WebSocketSession backendSession) {
                    backendSessions.put(sessionId, backendSession);
                    logger.info("Connected to backend WebSocket for session: {}", sessionId);
                }

                @Override
                public void handleMessage(WebSocketSession backendSession, org.springframework.web.socket.WebSocketMessage<?> message) {
                    // Proxy message from backend to Flutter
                    proxyMessageToFlutter(sessionId, message.getPayload().toString());
                }

                @Override
                public void handleTransportError(WebSocketSession backendSession, Throwable exception) {
                    logger.error("Backend WebSocket transport error for session {}: {}", sessionId, exception.getMessage());
                }

                @Override
                public void afterConnectionClosed(WebSocketSession backendSession, CloseStatus closeStatus) {
                    logger.info("Backend WebSocket closed for session {}: {}", sessionId, closeStatus);
                    backendSessions.remove(sessionId);
                    // Close Flutter session if backend disconnects
                    closeFlutterSession(sessionId, closeStatus);
                }

                @Override
                public boolean supportsPartialMessages() {
                    return false;
                }
            };

            CompletableFuture<WebSocketSession> future = client.execute(backendHandler, null, URI.create(backendWebSocketUrl));
            
            future.whenComplete((backendSession, throwable) -> {
                if (throwable != null) {
                    logger.error("Failed to connect to backend WebSocket: {}", throwable.getMessage());
                    try {
                        sendErrorAndClose(flutterSession, "Backend connection failed", "BACKEND_ERROR");
                    } catch (IOException e) {
                        logger.error("Error sending backend error message: {}", e.getMessage());
                    }
                }
            });

        } catch (Exception e) {
            logger.error("Error connecting to backend WebSocket: {}", e.getMessage());
        }
    }

    private void proxyMessageToBackend(String sessionId, String message) {
        WebSocketSession backendSession = backendSessions.get(sessionId);
        if (backendSession != null && backendSession.isOpen()) {
            try {
                backendSession.sendMessage(new TextMessage(message));
                logger.debug("Proxied message to backend for session: {}", sessionId);
            } catch (IOException e) {
                logger.error("Error proxying message to backend: {}", e.getMessage());
            }
        } else {
            logger.warn("No backend session found for: {}", sessionId);
        }
    }

    private void proxyMessageToFlutter(String sessionId, String message) {
        WebSocketSession flutterSession = flutterSessions.get(sessionId);
        if (flutterSession != null && flutterSession.isOpen()) {
            try {
                flutterSession.sendMessage(new TextMessage(message));
                logger.debug("Proxied message to Flutter for session: {}", sessionId);
            } catch (IOException e) {
                logger.error("Error proxying message to Flutter: {}", e.getMessage());
            }
        }
    }

    private void handleAuthTimeout(String sessionId) {
        WebSocketSession session = flutterSessions.get(sessionId);
        if (session != null && session.isOpen()) {
            Boolean isAuthenticated = (Boolean) session.getAttributes().get("authenticated");
            if (!Boolean.TRUE.equals(isAuthenticated)) {
                logger.warn("Auth timeout for session: {}", sessionId);
                try {
                    com.example.bff.DTO.WebSocketMessage timeoutResponse = com.example.bff.DTO.WebSocketMessage.authTimeout();
                    sendMessage(session, timeoutResponse);
                    session.close(new CloseStatus(4001, "Authentication timeout"));
                } catch (IOException e) {
                    logger.error("Error closing session after auth timeout: {}", e.getMessage());
                }
            }
        }
        authTimeoutTasks.remove(sessionId);
    }

    private void cancelAuthTimeout(String sessionId) {
        ScheduledFuture<?> task = authTimeoutTasks.remove(sessionId);
        if (task != null) {
            task.cancel(false);
        }
    }

    private void sendMessage(WebSocketSession session, com.example.bff.DTO.WebSocketMessage message) throws IOException {
        if (session.isOpen()) {
            String json = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(json));
            logger.debug("Sent message to session {}: {}", session.getId(), json);
        }
    }

    private void sendErrorAndClose(WebSocketSession session, String message, String code) throws IOException {
        cancelAuthTimeout(session.getId());
        com.example.bff.DTO.WebSocketMessage errorResponse = com.example.bff.DTO.WebSocketMessage.authError(message, code);
        sendMessage(session, errorResponse);
        session.close(new CloseStatus(4000, message));
        cleanup(session.getId());
    }

    private void closeFlutterSession(String sessionId, CloseStatus status) {
        WebSocketSession flutterSession = flutterSessions.get(sessionId);
        if (flutterSession != null && flutterSession.isOpen()) {
            try {
                flutterSession.close(status);
            } catch (IOException e) {
                logger.error("Error closing Flutter session: {}", e.getMessage());
            }
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        logger.error("=== BFF WebSocket Transport Error ===");
        logger.error("Session: {}", session.getId());
        logger.error("Error: {}", exception.getMessage());
        logger.error("=====================================");
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) {
        String sessionId = session.getId();
        logger.info("=== BFF WebSocket Connection Closed ===");
        logger.info("Session: {}", sessionId);
        logger.info("Status: {}", closeStatus);
        logger.info("=======================================");
        
        cleanup(sessionId);
        
        // Close backend connection if exists
        WebSocketSession backendSession = backendSessions.remove(sessionId);
        if (backendSession != null && backendSession.isOpen()) {
            try {
                backendSession.close(closeStatus);
            } catch (IOException e) {
                logger.error("Error closing backend session: {}", e.getMessage());
            }
        }
    }

    private void cleanup(String sessionId) {
        flutterSessions.remove(sessionId);
        sessionUserIds.remove(sessionId);
        cancelAuthTimeout(sessionId);
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
}
