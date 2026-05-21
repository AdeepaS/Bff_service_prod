package com.example.bff.controller;

import com.example.bff.websocket.GatewayWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for monitoring gateway WebSocket connections
 */
@RestController
@RequestMapping("/BFF/api/gateway")
public class GatewayStatusController {

    private static final Logger logger = LoggerFactory.getLogger(GatewayStatusController.class);

    @Autowired
    private GatewayWebSocketHandler gatewayWebSocketHandler;

    /**
     * Get status of connected OrangePi gateway devices
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getGatewayStatus() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            int connectedCount = gatewayWebSocketHandler.getConnectedGatewayCount();
            Map<String, String> connectedDevices = gatewayWebSocketHandler.getConnectedDevices();
            
            response.put("success", true);
            response.put("connectedGatewayCount", connectedCount);
            response.put("connectedDevices", connectedDevices);
            response.put("timestamp", System.currentTimeMillis());
            
            logger.info("Gateway status requested - {} devices connected", connectedCount);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting gateway status: {}", e.getMessage());
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Health check for gateway WebSocket proxy
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "BFF Gateway WebSocket Proxy");
        response.put("connectedGateways", gatewayWebSocketHandler.getConnectedGatewayCount());
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }
}
