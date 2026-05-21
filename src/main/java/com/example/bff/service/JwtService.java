package com.example.bff.service;
import com.example.bff.DTO.TokenType;
import com.example.bff.config.RSAKeyRecord;
import com.example.bff.logger.LoggerAdapter;
import com.example.bff.utils.RequestUtils;
import com.example.bff.utils.TokenBlacklist;
import io.jsonwebtoken.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets; // Add this import
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Map;

@Service
public class JwtService {
    private final RSAKeyRecord rsaKeyRecord;
    private final LoggerAdapter logger;
    @Value("${jwt.rsa-public-key}")
    private String publicKeyUrl;

    @Value("${app.environment:dev}")
    private String environment;

    @Autowired
    private RequestUtils requestUtils;

    @Autowired
    private TokenBlacklist tokenBlacklist;

    public JwtService(RSAKeyRecord rsaKeyRecord, LoggerAdapter logger) {
        this.rsaKeyRecord = rsaKeyRecord;
        this.logger = logger;
    }

    // Method to fetch the public key from the Authentication Service
    private RSAPublicKey getPublicKey() throws Exception {
        System.out.println("Fetching public key from: " + publicKeyUrl);
        URL url = new URL(publicKeyUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.connect();

        try (InputStream inputStream = connection.getInputStream()) {
            // Assuming the public key is in PEM format and includes headers like "-----BEGIN PUBLIC KEY-----"
            String publicKeyPem = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            publicKeyPem = publicKeyPem.replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");

            byte[] encoded = java.util.Base64.getDecoder().decode(publicKeyPem);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            System.out.println("Public key fetched successfully.");
            return (RSAPublicKey) keyFactory.generatePublic(new java.security.spec.X509EncodedKeySpec(encoded));

        } catch (IOException e) {
            throw new RuntimeException("Failed to load public key", e);
        }
    }

    private HttpHeaders addCorrelationIdHeader(HttpHeaders headers) {
        HttpHeaders updatedHeaders = new HttpHeaders(headers);

        if (!headers.containsKey("X-Correlation-Id")) {
            String correlationId = requestUtils.generateCorrelationId();
            updatedHeaders.add("X-Correlation-Id", correlationId);
            logger.info("Correlation ID generated: {}", correlationId);  // Log the generated CorrelationId
        }

        return updatedHeaders;
    }

    public void doFilterInternal(HttpServletRequest request,
                                 HttpServletResponse response,
                                 FilterChain filterChain) throws ServletException, java.io.IOException {
        try {

            logger.info("[JwtAccessTokenFilter:doFilterInternal] :: Started");

            logger.info("[JwtAccessTokenFilter:doFilterInternal] Filtering the Http Request: {}", request.getRequestURI());

            final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

            // Check if the Authorization header is present and starts with "Bearer "
            if (authHeader == null || !authHeader.startsWith(TokenType.Bearer.name())) {
                logger.error("[JwtAccessTokenFilter:doFilterInternal] Missing or invalid Authorization header");
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized: Token is missing or invalid.");
                return;
            }

            // Extract the token from the Authorization header
            final String token = authHeader.substring(7);

            // Create a JWT decoder using the public RSA key
            JwtDecoder jwtDecoder = NimbusJwtDecoder.withPublicKey(rsaKeyRecord.rsaPublicKey()).build();

            // Decode and validate the token
            Jwt jwtToken = jwtDecoder.decode(token);
            logger.info("[JwtAccessTokenFilter:doFilterInternal] Token validated successfully");

            // Continue with the filter chain
            filterChain.doFilter(request, response);
        } catch (JwtValidationException jwtValidationException) {
            logger.error("[JwtAccessTokenFilter:doFilterInternal] JWT validation failed: {}", jwtValidationException.getMessage());
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid token: " + jwtValidationException.getMessage());
        } catch (HttpClientErrorException e) {
            logger.error("[ProxyService:forwardRequest] Client error forwarding request with Correlation ID: {}", e.getMessage());
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid token: " + e.getMessage());
        } catch (Exception e) {
            logger.error("[JwtAccessTokenFilter:doFilterInternal] Exception: {}", e.getMessage());
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
        }
    }

    public boolean isTokenValid(String token, String correlationId) {
        try {
            // Check if the token is blacklisted
            if (tokenBlacklist.isTokenBlacklisted(token)) {
                logger.error("[JwtAccessTokenFilter:isTokenValid] Token is blacklisted for Correlation ID: {}", correlationId);
                return false;
            }

            // Check the token's expiration time
            Instant expirationTime = getTokenExpiration(token);

            // Check if the expiration time is null
            if (expirationTime == null) {
                logger.error("[JwtAccessTokenFilter:isTokenValid] Token expiration time is null for Correlation ID: {}", correlationId);
                return false; // Token is invalid or does not have an expiration time
            }

            // Check if the token is expired
            if (expirationTime.isBefore(Instant.now())) {
                logger.error("[JwtAccessTokenFilter:isTokenValid] Token is expired for Correlation ID: {}", correlationId);
                return false; // Token is expired
            }

            logger.info("[JwtAccessTokenFilter:isTokenValid] Token validated successfully for Correlation ID: {}", correlationId);
            return true; // Token is valid
        } catch (HttpClientErrorException e) {
            logger.error("[ProxyService:isTokenValid] Client error validating token for Correlation ID: {}", correlationId, e);
            return false;
        } catch (Exception e) {
            logger.error("[JwtAccessTokenFilter:isTokenValid] Unexpected error during token validation for Correlation ID: {}", correlationId, e);
            return false;
        }
    }

    public Instant getTokenExpiration(String token) {
        try {
            // Create a JWT decoder using the public RSA key
            JwtDecoder jwtDecoder = NimbusJwtDecoder.withPublicKey(rsaKeyRecord.rsaPublicKey()).build();
            logger.error("[JwtAccessTokenFilter:getTokenExpiration] Token received: {}", token);
            Jwt decodedJwt = jwtDecoder.decode(token);

            Instant expirationTime = decodedJwt.getExpiresAt();
            if (expirationTime == null) {
                logger.error("[JwtAccessTokenFilter:getTokenExpiration] Token does not have an expiration time");
                return null;
            }

            return expirationTime; // Return the expiration time
        } catch (JwtValidationException jwtValidationException) {
            logger.error("[JwtAccessTokenFilter:getTokenExpiration] JWT validation failed: {}", jwtValidationException.getMessage());
            return null; // Return null if the token is invalid
        }
    }

    /**
     * Extract fingerprint from JWT token
     * @param token The JWT token
     * @return The fingerprint from the token, or null if not found
     */
    public String getTokenFingerprint(String token) {
        try {
            // Create a JWT decoder using the public RSA key
            JwtDecoder jwtDecoder = NimbusJwtDecoder.withPublicKey(rsaKeyRecord.rsaPublicKey()).build();

            Jwt decodedJwt = jwtDecoder.decode(token);

            // Get the fingerprint claim from the token
            Object fingerprintClaim = decodedJwt.getClaim("fingerprint");

            if (fingerprintClaim == null) {
                logger.warn("[JwtService:getTokenFingerprint] Token does not have a fingerprint claim");
                return null;
            }

            return fingerprintClaim.toString();
        } catch (JwtValidationException jwtValidationException) {
            logger.error("[JwtService:getTokenFingerprint] JWT validation failed: {}", jwtValidationException.getMessage());
            return null;
        } catch (Exception e) {
            logger.error("[JwtService:getTokenFingerprint] Error extracting fingerprint from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Get the fingerprint cookie name based on environment
     * @return The cookie name for fingerprint
     */
    public String getFingerprintCookieName() {
        boolean isProduction = "prod".equalsIgnoreCase(environment);
        return isProduction ? "__Secure-Fgp" : "Fgp";
    }

    public String getAccessTokenCookieName() {
        boolean isProduction = "prod".equalsIgnoreCase(environment);
        return isProduction ? "__Secure-AccessToken" : "AccessToken";
    }

    public String getUserNameCookieName() {
        boolean isProduction = "prod".equalsIgnoreCase(environment);
        return isProduction ? "__Secure-UserName" : "UserName";
    }

    public String getRefreshTokenCookieName() {
        boolean isProduction = "prod".equalsIgnoreCase(environment);
        return isProduction ? "__Secure-RefreshToken" : "RefreshToken";
    }

    public String getUserRoleCookieName() {
        boolean isProduction = "prod".equalsIgnoreCase(environment);
        return isProduction ? "__Secure-Role" : "Role";
    }

    /**
     * Verify that the token fingerprint matches the cookie fingerprint
     * @param token The JWT token
     * @param request The HTTP request containing cookies
     * @return true if fingerprints match, false otherwise
     */
    public boolean verifyTokenFingerprint(String token, HttpServletRequest request) {
        try {
            // Get fingerprint from token
            String tokenFingerprint = getTokenFingerprint(token);
            if (tokenFingerprint == null) {
                logger.warn("[JwtService:verifyTokenFingerprint] No fingerprint in token");
                return false;
            }

            // Get fingerprint from cookie
            String cookieName = getFingerprintCookieName();
            String cookieFingerprint = null;

            if (request.getCookies() != null) {
                for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                    if (cookieName.equals(cookie.getName())) {
                        cookieFingerprint = cookie.getValue();
                        break;
                    }
                }
            }

            if (cookieFingerprint == null) {
                logger.warn("[JwtService:verifyTokenFingerprint] No fingerprint cookie found");
                return false;
            }

            // Compare fingerprints
            boolean fingerprintsMatch = tokenFingerprint.equals(cookieFingerprint);

            if (!fingerprintsMatch) {
                logger.warn("[JwtService:verifyTokenFingerprint] Fingerprint mismatch - possible token theft");
            } else {
                logger.debug("[JwtService:verifyTokenFingerprint] Fingerprint verification successful");
            }

            return fingerprintsMatch;
        } catch (Exception e) {
            logger.error("[JwtService:verifyTokenFingerprint] Error verifying fingerprint: {}", e.getMessage());
            return false;
        }
    }
}

