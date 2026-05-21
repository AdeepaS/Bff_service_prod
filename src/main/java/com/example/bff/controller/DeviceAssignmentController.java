package com.example.bff.controller;

import com.example.bff.DTO.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Controller for forwarding device assignment requests to WebSocket Service
 * This handles communication between Main Service and WebSocket Service through BFF
 */
@RestController
@RequestMapping("/BFF/api/device-assignment")
public class DeviceAssignmentController {

    private static final Logger logger = LoggerFactory.getLogger(DeviceAssignmentController.class);

    private final RestTemplate restTemplate;

    @Value("${config.routes[3].url:http://localhost:8082}")
    private String websocketServiceUrl;

    public DeviceAssignmentController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Forward device assignment acknowledgement to WebSocket Service
     * Called by Main Service when a device is assigned/unassigned to a child
     * 
     * Flow: Main Service -> BFF -> WebSocket Service -> OrangePi Gateway
     */
    @PostMapping("/acknowledgement")
    public ResponseEntity<?> forwardDeviceAssignmentAcknowledgement(
            @RequestBody Map<String, Object> acknowledgementRequest) {
        
        logger.info("=== BFF: Received Device Assignment Acknowledgement Request ===");
        logger.info("Request payload: {}", acknowledgementRequest);

        try {
            // Validate required field
            String safenetDeviceId = (String) acknowledgementRequest.get("safenetDeviceId");
            if (safenetDeviceId == null || safenetDeviceId.isEmpty()) {
                logger.error("Missing safenetDeviceId in acknowledgement request");
                return ResponseEntity.badRequest().body(new ApiResponse<>(
                    false, 
                    HttpStatus.BAD_REQUEST.value(), 
                    "Missing safenetDeviceId", 
                    null
                ));
            }

            // Build the target URL for WebSocket Service
            String targetUrl = websocketServiceUrl + "/api/policy/device-assignment/acknowledgement";
            logger.info("Forwarding to WebSocket Service: {}", targetUrl);

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Create request entity
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(acknowledgementRequest, headers);

            // Forward to WebSocket Service
            ResponseEntity<String> response = restTemplate.exchange(
                targetUrl,
                HttpMethod.POST,
                requestEntity,
                String.class
            );

            logger.info("✓ BFF: Successfully forwarded acknowledgement to WebSocket Service");
            logger.info("WebSocket Service response status: {}", response.getStatusCode());
            logger.info("WebSocket Service response: {}", response.getBody());
            logger.info("=============================================================");

            return ResponseEntity.status(response.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response.getBody());

        } catch (Exception e) {
            logger.error("✗ BFF: Error forwarding device assignment acknowledgement: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(new ApiResponse<>(
                false,
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Failed to forward acknowledgement to WebSocket Service: " + e.getMessage(),
                null
            ));
        }
    }

    /**
     * Get device assignment WebSocket status from WebSocket Service
     */
    @GetMapping("/status")
    public ResponseEntity<?> getDeviceAssignmentStatus() {
        logger.info("=== BFF: Getting Device Assignment Status ===");

        try {
            String targetUrl = websocketServiceUrl + "/api/policy/device-assignment/status";
            logger.info("Forwarding status request to: {}", targetUrl);

            ResponseEntity<String> response = restTemplate.getForEntity(targetUrl, String.class);

            logger.info("✓ BFF: Retrieved device assignment status");
            return ResponseEntity.status(response.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response.getBody());

        } catch (Exception e) {
            logger.error("✗ BFF: Error getting device assignment status: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(new ApiResponse<>(
                false,
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Failed to get status from WebSocket Service: " + e.getMessage(),
                null
            ));
        }
    }

    /**
     * Health check for device assignment endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "status", "healthy",
            "service", "bff-device-assignment",
            "timestamp", System.currentTimeMillis()
        ));
    }
}
