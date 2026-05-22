package com.example.bff.service;
import com.example.bff.DTO.ApiResponse;
import com.example.bff.logger.LoggerAdapter;
import com.example.bff.utils.RequestUtils;
import com.example.bff.utils.TokenBlacklist;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;

@RestController
public class ProxyService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private RequestUtils requestUtils;

    @Autowired
    private TokenBlacklist tokenBlacklist;

    @Autowired
    private JwtService jwtService;

    @Value("${core.authentication.service.url}")
    private String authenticationServiceUrl;

    @Value("${core.main.service.url}")
    private String mainServiceUrl;

    @Value("${core.my.service.url}")
    private String myServiceUrl;

   public final LoggerAdapter logger;

    public ProxyService(LoggerAdapter logger) {
        this.logger = logger;
    }

    public ResponseEntity<?> forwardMultipartRequest(String url, HttpHeaders headers, MultiValueMap<String, Object> body) {
        String correlationId = headers.getFirst("X-Correlation-Id");
        logger.info("[ProxyService:forwardMultipartRequest] Forwarding multipart request to URL: {} with Correlation ID: {}", url, correlationId);

        // Extract token for validation
        String jwtToken = null;
        if (headers != null && headers.containsKey(HttpHeaders.AUTHORIZATION)) {
            String authHeaderValue = headers.getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeaderValue != null && authHeaderValue.startsWith("Bearer ")) {
                jwtToken = authHeaderValue.substring(7);
            }
        }

        // Validate token (if present)
        if (jwtToken != null && !jwtToken.isEmpty()) {
            if (!validateToken(jwtToken, correlationId)) {
                logger.error("[ProxyService:forwardMultipartRequest] Token validation failed for Correlation ID: {}", correlationId);
                return ResponseEntity.ok(new ApiResponse<>(false, HttpStatus.UNAUTHORIZED.value(), "Invalid token", null));
            }
            logger.info("[ProxyService:forwardMultipartRequest] Token validated successfully for Correlation ID: {}", correlationId);
        } else {
            logger.warn("[ProxyService:forwardMultipartRequest] No token provided for Correlation ID: {}", correlationId);
        }

        // Log request details
        logger.info("[ProxyService:forwardMultipartRequest] Request headers: {} for Correlation ID: {}", headers, correlationId);
        logger.info("[ProxyService:forwardMultipartRequest] Request body: {} for Correlation ID: {}", body, correlationId);

        try {
            // Set custom headers with null check
            logger.info("[ProxyService:forwardMultipartRequest] Before setting X-Origin header for Correlation ID: {}", correlationId);
            if (myServiceUrl != null && !myServiceUrl.isEmpty()) {
                headers.set("X-Origin", myServiceUrl);
                logger.info("[ProxyService:forwardMultipartRequest] Set X-Origin header to: {} for Correlation ID: {}", myServiceUrl, correlationId);
            } else {
                logger.warn("[ProxyService:forwardMultipartRequest] myServiceUrl is null or empty, skipping X-Origin header for Correlation ID: {}", correlationId);
            }

            // Create HttpEntity with the headers and body
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // Forward the request
            logger.info("[ProxyService:forwardMultipartRequest] Sending multipart request to backend URL: {} with Correlation ID: {}", url, correlationId);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
            logger.info("[ProxyService:forwardMultipartRequest] Received response from backend for Correlation ID: {}. Status: {}. Response: {}", correlationId, response.getStatusCode(), response.getBody());

            // Extract cookies from the backend response
            List<String> backendCookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);

            // Create response headers
            HttpHeaders responseHeaders = new HttpHeaders();
            if (backendCookies != null) {
                responseHeaders.put(HttpHeaders.SET_COOKIE, backendCookies);
                logger.info("[ProxyService:forwardMultipartRequest] Forwarding cookies: {} for Correlation ID: {}", backendCookies, correlationId);
            }

            return ResponseEntity.status(response.getStatusCode())
                    .headers(responseHeaders)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response.getBody());
        } catch (HttpClientErrorException e) {
            logger.error("[ProxyService:forwardMultipartRequest] Client error forwarding multipart request with Correlation ID: {}. Status: {}, Error: {}",
                    correlationId, e.getStatusCode(), e.getMessage(), e);
            logger.error("[ProxyService:forwardMultipartRequest] Backend error response body: {}", e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode())
                    .body(new ApiResponse<>(false, e.getStatusCode().value(), "Client error occurred: " + e.getMessage(), null));
        } catch (HttpServerErrorException e) {
            logger.error("[ProxyService:forwardMultipartRequest] Server error forwarding multipart request with Correlation ID: {}. Status: {}, Error: {}",
                    correlationId, e.getStatusCode(), e.getMessage(), e);
            logger.error("[ProxyService:forwardMultipartRequest] Backend error response body: {}", e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode())
                    .body(new ApiResponse<>(false, e.getStatusCode().value(), "Server error occurred: " + e.getMessage(), null));
        } catch (Exception e) {
            logger.error("[ProxyService:forwardMultipartRequest] Unexpected error forwarding multipart request with Correlation ID: {}. Error: {}",
                    correlationId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal server error: " + e.getMessage(), null));
        }
    }

    public boolean validateToken(String token , String correlationId) {
        logger.info("[ProxyService:validToken] Validating started for: {}", correlationId);
        try {
            boolean isValid = jwtService.isTokenValid(token, correlationId);
            if (isValid) {
                logger.info("[ProxyService:validToken] JWT token is valid: {}", correlationId);
                return true;
            } else {
                logger.error("[ProxyService:validToken] JWT token is invalid: {}", correlationId);
                return false;
            }
        } catch (Exception e) {
            logger.error("[ProxyService:validToken] Unexpected error during JWT validation for {}: ",correlationId, e);
            return false;
        }
    }

    // Helper method to check if the request is for an Auth endpoint
    public boolean isAuthEndpoint(String requestUri) {
        return requestUri.contains("/sign-up") || requestUri.contains("/add_permission")
                || requestUri.contains("/verify-otp") || requestUri.contains("/resend-otp")
                || requestUri.contains("/first-login-reset") || requestUri.contains("/reset-forgotten-password")
                || requestUri.contains("/reset-password") || requestUri.contains("/forgot-password")
                || requestUri.contains("/setup-password");
    }

    public boolean isAdminEndpoint(String requestUri) {
        return requestUri.startsWith("/admin/");
    }

    public boolean isRefreshToken(String requestUri) {
        return requestUri.contains("/refresh-token");
    }

    public boolean isAuthApiEndpoint(String requestUri) {
        return requestUri.contains("/view/allUsers") || requestUri.contains("/update/user") || requestUri.contains("/delete/user")
                || requestUri.contains("/create/user") || requestUri.contains("/update/userPassword") || requestUri.contains("/update/firstLoginReset") || requestUri.contains("/email/");
    }

    public boolean isLogin(String requestUri) {
        return requestUri.contains("/sign-in") || requestUri.contains("/login");
    }

    public boolean isSignup(String requestUri) {
        return requestUri.contains("/sign-up");
    }


    public boolean isLogout(String requestUri) {
        return requestUri.contains("/logout");
    }

    public  boolean isMainServiceEndpoint(String requestUri) { return requestUri.contains("/api");}

    public ResponseEntity<?> forwardRequest(
            String backendUrl,
            HttpMethod method,
            HttpHeaders headers,
            String token,
            Object body,
            boolean validateToken) {

        String correlationId = headers.getFirst("X-Correlation-Id");

        // Handle token-based authorization
        if (headers != null && headers.containsKey(HttpHeaders.AUTHORIZATION)) {
            String authHeaderValue = headers.getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeaderValue != null && authHeaderValue.startsWith("Bearer ")) {
                String jwtToken = authHeaderValue.substring(7);

                if (validateToken) {
                    if (jwtToken.isEmpty()) {
                        logger.error("[ProxyService:forwardRequest] Token is missing for: {}", correlationId);
                        return ResponseEntity.ok(new ApiResponse<>(false, HttpStatus.UNAUTHORIZED.value(), "Authorization token is missing", null));
                    }

                    if (!validateToken(jwtToken ,correlationId )) {
                        logger.error("[ProxyService:forwardRequest] Token validation failed: {}", correlationId);
                        return ResponseEntity.ok(new ApiResponse<>(false, HttpStatus.UNAUTHORIZED.value(), "Invalid token", null));
                    }
                }
            }
        }

        // Set custom headers
        headers.set("X-Origin", myServiceUrl);

        logger.info("[ProxyService:forwardRequest] Forwarding request with Correlation ID: {}", correlationId);

        HttpEntity<Object> entity = new HttpEntity<>(body, headers);

        try {
            // Forward the request
            logger.info("[ProxyService:forwardRequest] Sending request to backend URL: {} with Correlation ID: {}", backendUrl, correlationId);
            logger.info("[ProxyService:forwardRequest] Sending request to backend URL Headers: {} ", headers);
            ResponseEntity<String> response = restTemplate.exchange(backendUrl, method, entity, String.class);
            logger.error("[ProxyService:forwardRequest] response 1: {}", response);
            // Extract cookies from the backend response
            List<String> backendCookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);

            // Create response headers
            HttpHeaders responseHeaders = new HttpHeaders();
            if (backendCookies != null) {
                // Ensure cookies are properly forwarded without modification
                responseHeaders.put(HttpHeaders.SET_COOKIE, backendCookies);
                logger.info("[ProxyService:forwardRequest] Forwarding cookies: {}", backendCookies);

            }

            // Log the response along with the Correlation ID
            logger.info("[ProxyService:forwardRequest] Received response from backend for Correlation ID: {}. Status: {}. Response: {}.", correlationId, response.getStatusCode(), response.getBody());

            return ResponseEntity.status(response.getStatusCode())
                    .headers(responseHeaders)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response.getBody());
        } catch (HttpClientErrorException e) {
            logger.error("[ProxyService:forwardRequest] Client error forwarding request with Correlation ID: {}", correlationId);

            // Try to parse the error response body from the backend
            String errorResponseBody = e.getResponseBodyAsString();

            if (errorResponseBody != null && !errorResponseBody.isEmpty()) {
                try {
                    // If backend returns ApiResponseDto, forward it as-is
                    ObjectMapper objectMapper = new ObjectMapper();
                    JsonNode jsonNode = objectMapper.readTree(errorResponseBody);

                    // Check if it's a valid ApiResponseDto structure
                    if (jsonNode.has("success") && jsonNode.has("statusCode") && jsonNode.has("message")) {
                        return ResponseEntity.status(e.getStatusCode())
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(errorResponseBody);
                    }
                } catch (Exception parseException) {
                    logger.warn("[ProxyService:forwardRequest] Failed to parse backend error response: {}", parseException.getMessage());
                }
            }

            // Fallback to custom error messages
            String errorMessage = getClientErrorMessage(e.getStatusCode());
            return ResponseEntity.status(e.getStatusCode())
                    .body(new ApiResponse<>(false, e.getStatusCode().value(), errorMessage, null));
        } catch (HttpServerErrorException e) {
            logger.error("[ProxyService:forwardRequest] Server error forwarding request with Correlation ID: {}. Status: {}, Error: {}",
                    correlationId, e.getStatusCode(), e.getMessage(), e);

            // Try to parse the error response body from the backend
            String errorResponseBody = e.getResponseBodyAsString();
            logger.error("[ProxyService:forwardRequest] Backend error response body: {}", errorResponseBody);

            if (errorResponseBody != null && !errorResponseBody.isEmpty()) {
                try {
                    // If backend returns ApiResponseDto, forward it as-is
                    ObjectMapper objectMapper = new ObjectMapper();
                    JsonNode jsonNode = objectMapper.readTree(errorResponseBody);

                    // Check if it's a valid ApiResponseDto structure
                    if (jsonNode.has("success") && jsonNode.has("statusCode") && jsonNode.has("message")) {
                        return ResponseEntity.status(e.getStatusCode())
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(errorResponseBody);
                    }
                } catch (Exception parseException) {
                    logger.warn("[ProxyService:forwardRequest] Failed to parse backend error response: {}", parseException.getMessage());
                }
            }

            return ResponseEntity.status(e.getStatusCode())
                    .body(new ApiResponse<>(false, e.getStatusCode().value(), "Server error occurred: " + e.getMessage(), null));
        } catch (Exception e) {
            logger.error("[ProxyService:forwardRequest] Error forwarding request with Correlation ID: {}. Error: {}",
                    correlationId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal server error: " + e.getMessage(), null));
        }
    }

    private String getClientErrorMessage(HttpStatusCode statusCode) {
        if (statusCode == HttpStatus.FORBIDDEN) {
            return "Access denied: You don't have permission to access this resource";
        } else if (statusCode == HttpStatus.UNAUTHORIZED) {
            return "Authentication failed: Invalid or expired token";
        } else if (statusCode == HttpStatus.BAD_REQUEST) {
            return "Bad request: Please check your request parameters";
        } else if (statusCode == HttpStatus.NOT_FOUND) {
            return "Resource not found";
        } else {
            return "Client error occurred";
        }
    }



    // Wrapper for forwarding request with token
    public ResponseEntity<?> forwardRequestWithToken(
            String backendUrl, HttpHeaders authHeader, Object body, HttpMethod method) {
        return forwardRequest(backendUrl, method, authHeader,null, body, true);
    }

    public ResponseEntity<?> forwardRequestWithToken(
            String backendUrl, HttpHeaders authHeader, HttpMethod method) {
        return forwardRequest(backendUrl, method, authHeader, null ,null, true);
    }

    public ResponseEntity<?> forwardRequestWithTokenForLogout(
            String backendUrl, HttpHeaders authHeader, HttpMethod method, String correlationId) {
        logger.info("[ProxyService:forwardRequestWithTokenForLogout] Starting logout request processing for Correlation ID: {}", correlationId);

        // Extract JWT token
        String authHeaderValue = authHeader.getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeaderValue == null || !authHeaderValue.startsWith("Bearer ")) {
            logger.error("[ProxyService:forwardRequestWithTokenForLogout] Missing or invalid Authorization header for Correlation ID: {}", correlationId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(false, HttpStatus.BAD_REQUEST.value(), "Invalid or missing Authorization header", null));
        }

        String jwtToken = authHeaderValue.substring(7); // Extract the JWT token
        logger.info("[ProxyService:forwardRequestWithTokenForLogout] Extracted JWT token for Correlation ID: {}", correlationId);

        // Forward the request to the backend service
        logger.info("[ProxyService:forwardRequestWithTokenForLogout] Forwarding logout request to backend URL: {} for Correlation ID: {}", backendUrl, correlationId);
        ResponseEntity<?> response = forwardRequest(backendUrl, method, authHeader, null, null, true);
        logger.info("[ProxyService:forwardRequestWithTokenForLogout] Received response with status code: {} from backend for Correlation ID: {}", response.getStatusCode(), correlationId);

        // Handle the response
        if (response.getStatusCode().is2xxSuccessful()) {
            logger.info("[ProxyService:forwardRequestWithTokenForLogout] Logout request was successful for Correlation ID: {}", correlationId);

            // Add the token to the blacklist
            try {
                Instant expirationTime = jwtService.getTokenExpiration(jwtToken);
                long expirationEpochMillis = expirationTime.toEpochMilli();
                tokenBlacklist.addToBlacklist(jwtToken, expirationEpochMillis);
                logger.info("[ProxyService:forwardRequestWithTokenForLogout] Token successfully added to blacklist with expiration time: {} for Correlation ID: {}", expirationEpochMillis, correlationId);
            } catch (Exception e) {
                logger.error("[ProxyService:forwardRequestWithTokenForLogout] Error while adding token to blacklist for Correlation ID: {}: {}", correlationId, e.getMessage(), e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new ApiResponse<>(false, HttpStatus.INTERNAL_SERVER_ERROR.value(), "Error during token blacklisting", null));
            }
        } else {
            logger.error("[ProxyService:forwardRequestWithTokenForLogout] Logout request failed with status code: {} for Correlation ID: {}", response.getStatusCode(), correlationId);
        }

        // Return the response from the backend service
        logger.info("[ProxyService:forwardRequestWithTokenForLogout] Logout request processing completed for Correlation ID: {}", correlationId);
        return response;
    }



    // Wrapper for forwarding request without token
    public ResponseEntity<?> forwardRequestWithoutToken(
            String backendUrl, HttpMethod method) {
        return forwardRequest(backendUrl, method, null,null, null, false);
    }

    // Wrapper for forwarding request without token (with body)
    public ResponseEntity<?> forwardRequestWithoutToken(
            String backendUrl, HttpMethod method,HttpHeaders authHeader, Object body) {
        return forwardRequest(backendUrl, method, authHeader,null, body, false);
    }


    public ResponseEntity<?> forwardRequestWithBasicAuth(
            String backendUrl, HttpMethod method, HttpHeaders authHeader) {
        return forwardRequest(backendUrl, method, authHeader,null, null, false);
    }

    public ResponseEntity<?> forwardRequestWithAuthBody(
            String backendUrl, HttpMethod method, Object body) {
        return forwardRequest(backendUrl, method, null,null, body, false);
    }

    public boolean isOtpVerification(String requestUri) {
        return requestUri.equals("/auth/verify-otp") ||
                requestUri.equals("/auth/verify-otp-login");
    }
}
