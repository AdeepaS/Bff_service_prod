package com.example.bff.utils;

import com.example.bff.security.DecipheredTokenRequestWrapper;
import com.example.bff.security.TokenDecryptionUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * Utility class for handling fingerprint cookies
 */
@Component
public class FingerprintUtils {
    private static final Logger logger = LoggerFactory.getLogger(FingerprintUtils.class);

    @Value("${app.environment:dev}")
    private String environment;

    @Value("${app.cookie.max-age:86400}")
    private int cookieMaxAge; // Default: 1 day in seconds

    @Value("${app.cookie.path:/}")
    private String cookiePath;

    @Value("${app.cookie.same-site:Lax}")
    private String cookieSameSite;

    public void setFingerprintCookie(HttpServletResponse response, String fingerprint) {
        logger.info("[FingerprintUtils:setFingerprintCookie] Environment ={}" , environment);
        boolean isProduction = "prod".equalsIgnoreCase(environment);
        logger.info("[FingerprintUtils:setFingerprintCookie] isProduction ={}" , isProduction);
        String cookieName = isProduction ? "__Secure-Fgp" : "Fgp";

        Cookie fingerprintCookie = new Cookie(cookieName, fingerprint);
        fingerprintCookie.setHttpOnly(true);
        fingerprintCookie.setSecure(isProduction); // Only set to true in production (HTTPS)
        fingerprintCookie.setPath(cookiePath);
        fingerprintCookie.setMaxAge(cookieMaxAge);

        // Set SameSite attribute using header
        String sameSite = cookieSameSite;
        String cookieHeader = String.format("%s=%s; Max-Age=%d; Path=%s; HttpOnly; %sSameSite=%s",
                cookieName,
                fingerprint,
                cookieMaxAge,
                cookiePath,
                isProduction ? "Secure; " : "",
                sameSite);

        response.setHeader("Set-Cookie", cookieHeader);

        logger.info("[FingerprintUtils:setFingerprintCookie] Fingerprint cookie set: name={}, secure={}, httpOnly=true, sameSite={}",
                cookieName, isProduction, sameSite);
    }


    public void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        boolean isProduction = "prod".equalsIgnoreCase(environment);
        String cookieName = isProduction ? "__Secure-RefreshToken" : "RefreshToken";

        Cookie cookie = new Cookie(cookieName, refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(isProduction); // Only set to true in production (HTTPS)
        cookie.setPath(cookiePath);
        cookie.setMaxAge(cookieMaxAge);

        // Set SameSite attribute using header
        String sameSite = cookieSameSite;
        String cookieHeader = String.format("%s=%s; Max-Age=%d; Path=%s; HttpOnly; %sSameSite=%s",
                cookieName,
                refreshToken,
                cookieMaxAge,
                cookiePath,
                isProduction ? "Secure; " : "",
                sameSite);

        response.addHeader("Set-Cookie", cookieHeader);

        logger.info("[FingerprintUtils:setRefreshTokenCookie] RefreshToken cookie set: name={}, secure={}, httpOnly=true, sameSite={}",
                cookieName, isProduction, sameSite);
    }

    public void setAccessTokenCookie(HttpServletResponse response, String accessToken) {
        boolean isProduction = "prod".equalsIgnoreCase(environment);
        String cookieName = isProduction ? "__Secure-AccessToken" : "AccessToken";

        Cookie cookie = new Cookie(cookieName, accessToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(isProduction);
        cookie.setPath(cookiePath);
        cookie.setMaxAge(cookieMaxAge);

        // Set SameSite attribute using header
        String sameSite = cookieSameSite;
        String cookieHeader = String.format("%s=%s; Max-Age=%d; Path=%s; HttpOnly; %sSameSite=%s",
                cookieName,
                accessToken,
                cookieMaxAge,
                cookiePath,
                isProduction ? "Secure; " : "",
                sameSite);

        response.addHeader("Set-Cookie", cookieHeader);

        logger.info("[FingerprintUtils:setAccessTokenCookie] AccessToken cookie set: name={}, secure={}, httpOnly=true, sameSite={}",
                cookieName, isProduction, sameSite);
    }

    public void setUserNameCookie(HttpServletResponse response, String userName) {
        boolean isProduction = "prod".equalsIgnoreCase(environment);
        String cookieName = isProduction ? "__Secure-UserName" : "UserName";

        Cookie cookie = new Cookie(cookieName, userName);
        cookie.setHttpOnly(true);
        cookie.setSecure(isProduction);
        cookie.setPath(cookiePath);
        cookie.setMaxAge(cookieMaxAge);

        // Set SameSite attribute using header
        String sameSite = cookieSameSite;
        String cookieHeader = String.format("%s=%s; Max-Age=%d; Path=%s; HttpOnly; %sSameSite=%s",
                cookieName,
                userName,
                cookieMaxAge,
                cookiePath,
                isProduction ? "Secure; " : "",
                sameSite);

        response.addHeader("Set-Cookie", cookieHeader);

        logger.info("[FingerprintUtils:setUserNameCookie] UserName cookie set: name={}, secure={}, httpOnly=true, sameSite={}",
                cookieName, isProduction, sameSite);
    }

    public void setRoleCookie(HttpServletResponse response, String role) {
        boolean isProduction = "prod".equalsIgnoreCase(environment);
        String cookieName = isProduction ? "__Secure-Role" : "Role";

        Cookie cookie = new Cookie(cookieName, role);
        cookie.setHttpOnly(true);
        cookie.setSecure(isProduction);
        cookie.setPath(cookiePath);
        cookie.setMaxAge(cookieMaxAge);

        // Set SameSite attribute using header
        String sameSite = cookieSameSite;
        String cookieHeader = String.format("%s=%s; Max-Age=%d; Path=%s; HttpOnly; %sSameSite=%s",
                cookieName,
                role,
                cookieMaxAge,
                cookiePath,
                isProduction ? "Secure; " : "",
                sameSite);

        response.addHeader("Set-Cookie", cookieHeader);

        logger.info("[FingerprintUtils:setRoleCookie] Role cookie set: name={}, secure={}, httpOnly=true, sameSite={}",
                cookieName, isProduction, sameSite);
    }

    public void setRoleIDCookie(HttpServletResponse response, Long roleId) {
        boolean isProduction = "prod".equalsIgnoreCase(environment);
        String cookieName = isProduction ? "__Secure-RoleId" : "RoleId";

        Cookie cookie = new Cookie(cookieName, String.valueOf(roleId));
        cookie.setHttpOnly(true);
        cookie.setSecure(isProduction);
        cookie.setPath(cookiePath);
        cookie.setMaxAge(cookieMaxAge);

        // Set SameSite attribute using header
        String sameSite = cookieSameSite;
        String cookieHeader = String.format("%s=%s; Max-Age=%d; Path=%s; HttpOnly; %sSameSite=%s",
                cookieName,
                roleId,
                cookieMaxAge,
                cookiePath,
                isProduction ? "Secure; " : "",
                sameSite);

        response.addHeader("Set-Cookie", cookieHeader);

        logger.info("[FingerprintUtils:setRoleCookie] Role cookie set: name={}, secure={}, httpOnly=true, sameSite={}",
                cookieName, isProduction, sameSite);
    }

    public void setWorkFlowRoleIDCookie(HttpServletResponse response, Long roleId) {
        boolean isProduction = "prod".equalsIgnoreCase(environment);
        String cookieName = isProduction ? "__Secure-workFlowRoleId" : "workFlowRoleId";

        Cookie cookie = new Cookie(cookieName, String.valueOf(roleId));
        cookie.setHttpOnly(true);
        cookie.setSecure(isProduction);
        cookie.setPath(cookiePath);
        cookie.setMaxAge(cookieMaxAge);

        // Set SameSite attribute using header
        String sameSite = cookieSameSite;
        String cookieHeader = String.format("%s=%s; Max-Age=%d; Path=%s; HttpOnly; %sSameSite=%s",
                cookieName,
                roleId,
                cookieMaxAge,
                cookiePath,
                isProduction ? "Secure; " : "",
                sameSite);

        response.addHeader("Set-Cookie", cookieHeader);

        logger.info("[FingerprintUtils:setRoleCookie] workFlowRoleId cookie set: name={}, secure={}, httpOnly=true, sameSite={}",
                cookieName, isProduction, sameSite);
    }

    public void clearAllAuthCookies(HttpServletResponse response) {
        clearFingerprintCookie(response);
        clearRefreshTokenCookie(response);
        clearAccessTokenCookie(response);
        clearUserNameCookie(response);
        clearRoleCookie(response);
        clearRoleIdCookie(response);
        clearWorkFlowRoleIdCookie(response);

        logger.info("[FingerprintUtils:clearAllAuthCookies] All authentication cookies cleared");
    }

    public void clearFingerprintCookie(HttpServletResponse response) {
        boolean isProduction = "prod".equalsIgnoreCase(environment);
        String cookieName = isProduction ? "__Secure-Fgp" : "Fgp";

        Cookie fingerprintCookie = new Cookie(cookieName, "");
        fingerprintCookie.setHttpOnly(true);
        fingerprintCookie.setSecure(isProduction);
        fingerprintCookie.setPath(cookiePath);
        fingerprintCookie.setMaxAge(0); // Expire immediately

        // Set SameSite attribute using header
        String sameSite = cookieSameSite;
        String cookieHeader = String.format("%s=; Max-Age=0; Path=%s; HttpOnly; %sSameSite=%s",
                cookieName,
                cookiePath,
                isProduction ? "Secure; " : "",
                sameSite);

        response.setHeader("Set-Cookie", cookieHeader);

        logger.info("[FingerprintUtils:clearFingerprintCookie] Fingerprint cookie cleared");
    }

    public void clearRefreshTokenCookie(HttpServletResponse response) {
        boolean isProduction = "prod".equalsIgnoreCase(environment);
        String cookieName = isProduction ? "__Secure-RefreshToken" : "RefreshToken";

        Cookie cookie = new Cookie(cookieName, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(isProduction);
        cookie.setPath(cookiePath);
        cookie.setMaxAge(0); // Expire immediately

        // Set SameSite attribute using header
        String sameSite = cookieSameSite;
        String cookieHeader = String.format("%s=; Max-Age=0; Path=%s; HttpOnly; %sSameSite=%s",
                cookieName,
                cookiePath,
                isProduction ? "Secure; " : "",
                sameSite);

        response.addHeader("Set-Cookie", cookieHeader);

        logger.info("[FingerprintUtils:clearRefreshTokenCookie] RefreshToken cookie cleared");
    }

    public void clearAccessTokenCookie(HttpServletResponse response) {
        boolean isProduction = "prod".equalsIgnoreCase(environment);
        String cookieName = isProduction ? "__Secure-AccessToken" : "AccessToken";

        Cookie cookie = new Cookie(cookieName, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(isProduction);
        cookie.setPath(cookiePath);
        cookie.setMaxAge(0); // Expire immediately

        // Set SameSite attribute using header
        String sameSite = cookieSameSite;
        String cookieHeader = String.format("%s=; Max-Age=0; Path=%s; HttpOnly; %sSameSite=%s",
                cookieName,
                cookiePath,
                isProduction ? "Secure; " : "",
                sameSite);

        response.addHeader("Set-Cookie", cookieHeader);

        logger.info("[FingerprintUtils:clearAccessTokenCookie] AccessToken cookie cleared");
    }

    public void clearUserNameCookie(HttpServletResponse response) {
        boolean isProduction = "prod".equalsIgnoreCase(environment);
        String cookieName = isProduction ? "__Secure-UserName" : "UserName";

        Cookie cookie = new Cookie(cookieName, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(isProduction);
        cookie.setPath(cookiePath);
        cookie.setMaxAge(0); // Expire immediately

        // Set SameSite attribute using header
        String sameSite = cookieSameSite;
        String cookieHeader = String.format("%s=; Max-Age=0; Path=%s; HttpOnly; %sSameSite=%s",
                cookieName,
                cookiePath,
                isProduction ? "Secure; " : "",
                sameSite);

        response.addHeader("Set-Cookie", cookieHeader);

        logger.info("[FingerprintUtils:clearUserNameCookie] UserName cookie cleared");
    }

    public void clearRoleCookie(HttpServletResponse response) {
        boolean isProduction = "prod".equalsIgnoreCase(environment);
        String cookieName = isProduction ? "__Secure-Role" : "Role";

        Cookie cookie = new Cookie(cookieName, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(isProduction);
        cookie.setPath(cookiePath);
        cookie.setMaxAge(0); // Expire immediately

        // Set SameSite attribute using header
        String sameSite = cookieSameSite;
        String cookieHeader = String.format("%s=; Max-Age=0; Path=%s; HttpOnly; %sSameSite=%s",
                cookieName,
                cookiePath,
                isProduction ? "Secure; " : "",
                sameSite);

        response.addHeader("Set-Cookie", cookieHeader);

        logger.info("[FingerprintUtils:clearRoleCookie] Role cookie cleared");
    }

    public void clearRoleIdCookie(HttpServletResponse response) {
        boolean isProduction = "prod".equalsIgnoreCase(environment);
        String cookieName = isProduction ? "__Secure-RoleId" : "RoleId";

        Cookie cookie = new Cookie(cookieName, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(isProduction);
        cookie.setPath(cookiePath);
        cookie.setMaxAge(0); // Expire immediately

        // Set SameSite attribute using header
        String sameSite = cookieSameSite;
        String cookieHeader = String.format("%s=; Max-Age=0; Path=%s; HttpOnly; %sSameSite=%s",
                cookieName,
                cookiePath,
                isProduction ? "Secure; " : "",
                sameSite);

        response.addHeader("Set-Cookie", cookieHeader);

        logger.info("[FingerprintUtils:clearFingerprintCookie] RoleId cookie cleared");
    }

    public void clearWorkFlowRoleIdCookie(HttpServletResponse response) {
        boolean isProduction = "prod".equalsIgnoreCase(environment);
        String cookieName = isProduction ? "__Secure-workFlowRoleId" : "workFlowRoleId";

        Cookie cookie = new Cookie(cookieName, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(isProduction);
        cookie.setPath(cookiePath);
        cookie.setMaxAge(0); // Expire immediately

        // Set SameSite attribute using header
        String sameSite = cookieSameSite;
        String cookieHeader = String.format("%s=; Max-Age=0; Path=%s; HttpOnly; %sSameSite=%s",
                cookieName,
                cookiePath,
                isProduction ? "Secure; " : "",
                sameSite);

        response.addHeader("Set-Cookie", cookieHeader);

        logger.info("[FingerprintUtils:clearFingerprintCookie] RoleId cookie cleared");
    }

    /**
     * Process logout request: Extract refresh token from cookies, decrypt it, and add to headers
     * @param request The HTTP request
     * @param headers The HTTP headers to be modified
     * @param correlationId The correlation ID for logging
     * @return True if refresh token was successfully processed, false otherwise
     */
    public boolean processLogoutRequest(HttpServletRequest request, HttpHeaders headers, String correlationId) {
        logger.info("[ProxyController:processLogoutRequest] Processing logout request for Correlation ID: {}", correlationId);

        // Determine cookie name based on environment
        boolean isProduction = "prod".equalsIgnoreCase(environment);
        String refreshTokenCookieName = isProduction ? "__Secure-RefreshToken" : "RefreshToken";
        String encryptedRefreshToken = null;

        // Extract refresh token from cookies
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (refreshTokenCookieName.equals(cookie.getName())) {
                    encryptedRefreshToken = cookie.getValue();
                    logger.info("[ProxyController:processLogoutRequest] Found refresh token in cookies for Correlation ID: {}", correlationId);
                    break;
                }
            }
        }

        if (encryptedRefreshToken == null || encryptedRefreshToken.isEmpty()) {
            logger.error("[ProxyController:processLogoutRequest] No refresh token found in cookies for logout, Correlation ID: {}", correlationId);
            return false;
        }

        try {
            // Add the encrypted refresh token to the headers
            headers.set("refreshToken", "Bearer " + encryptedRefreshToken);
            logger.info("[ProxyController:processLogoutRequest] Encrypted refresh token added to headers for logout, Correlation ID: {}", correlationId);

            return true;
        } catch (Exception e) {
            logger.error("[ProxyController:processLogoutRequest] Error processing refresh token: {}, Correlation ID: {}",
                    e.getMessage(), correlationId);
            return false;
        }
    }

    /**
     * Process logout response: Clear all authentication cookies if logout was successful
     * @param logoutResponse The response from the auth service
     * @param response The HTTP response to modify (add cookie clearing headers)
     * @param correlationId The correlation ID for logging
     * @return The final response to return to the client
     */
    public ResponseEntity<?> processLogoutResponse(ResponseEntity<?> logoutResponse,
                                                    HttpServletResponse response,
                                                    String correlationId) {
        logger.info("[ProxyController:processLogoutResponse] Processing logout response with status: {}, Correlation ID: {}",
                logoutResponse.getStatusCode(), correlationId);

        // If logout was successful, clear all cookies
        if (logoutResponse.getStatusCode().is2xxSuccessful()) {
            logger.info("[ProxyController:processLogoutResponse] Logout successful, clearing all auth cookies, Correlation ID: {}", correlationId);

            // Clear all authentication cookies
            clearAllAuthCookies(response);

            // Check if the response body contains success information
            Object responseBody = logoutResponse.getBody();
            if (responseBody != null) {
                try {
                    // If the response is a string, try to parse it as JSON
                    if (responseBody instanceof String) {
                        ObjectMapper objectMapper = new ObjectMapper();
                        JsonNode rootNode = objectMapper.readTree((String) responseBody);

                        // Check if response indicates success
                        if (rootNode.has("success") && rootNode.get("success").asBoolean()) {
                            logger.info("[ProxyController:processLogoutResponse] Auth service confirmed successful logout, Correlation ID: {}", correlationId);
                        } else {
                            logger.warn("[ProxyController:processLogoutResponse] Auth service returned non-success response but with 2xx status, Correlation ID: {}", correlationId);
                        }
                    }
                } catch (Exception e) {
                    logger.warn("[ProxyController:processLogoutResponse] Could not parse response body: {}, Correlation ID: {}",
                            e.getMessage(), correlationId);
                }
            }
        } else {
            logger.warn("[ProxyController:processLogoutResponse] Logout request was not successful, status: {}, Correlation ID: {}",
                    logoutResponse.getStatusCode(), correlationId);
        }

        return logoutResponse;
    }

    /**
     * Process get access token by refreshToken request: Extract refresh token from cookies, decrypt it, and add to headers
     * @param request The HTTP request
     * @param headers The HTTP headers to be modified
     * @param correlationId The correlation ID for logging
     * @return True if refresh token was successfully processed, false otherwise
     */
    public boolean processRefreshTokenRequest(HttpServletRequest request, HttpHeaders headers, String correlationId) {
        logger.info("[ProxyController:processLogoutRequest] Processing refresh token request for Correlation ID: {}", correlationId);

        // Determine cookie name based on environment
        boolean isProduction = "prod".equalsIgnoreCase(environment);
        String refreshTokenCookieName = isProduction ? "__Secure-RefreshToken" : "RefreshToken";
        String encryptedRefreshToken = null;

        // Extract refresh token from cookies
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (refreshTokenCookieName.equals(cookie.getName())) {
                    encryptedRefreshToken = cookie.getValue();
                    logger.info("[ProxyController:processLogoutRequest] Found refresh token in cookies for Correlation ID: {}", correlationId);
                    break;
                }
            }
        }

        if (encryptedRefreshToken == null || encryptedRefreshToken.isEmpty()) {
            logger.error("[ProxyController:processLogoutRequest] No refresh token found in cookies for logout, Correlation ID: {}", correlationId);
            return false;
        }

        try {
            // Add the encrypted refresh token to the headers
            headers.set("AuthorizationRefreshToken", "Bearer " + encryptedRefreshToken);
            logger.info("[ProxyController:processLogoutRequest] Encrypted refresh token added to headers for logout, Correlation ID: {}", correlationId);

            return true;
        } catch (Exception e) {
            logger.error("[ProxyController:processLogoutRequest] Error processing refresh token: {}, Correlation ID: {}",
                    e.getMessage(), correlationId);
            return false;
        }
    }

    public boolean processRequestWithAddingTokenForAuthService(HttpServletRequest request, HttpHeaders headers, String correlationId) {
        logger.info("[ProxyController:processLogoutRequest] Processing refresh token request for Correlation ID: {}", correlationId);

        // Determine cookie name based on environment
        boolean isProduction = "prod".equalsIgnoreCase(environment);
        String cookieName = isProduction ? "__Secure-AccessToken" : "AccessToken";
        String encryptedAccessToken = null;

        // Extract refresh token from cookies
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (cookieName.equals(cookie.getName())) {
                    encryptedAccessToken = cookie.getValue();
                    logger.info("[ProxyController:processLogoutRequest] Found Access token in cookies for Correlation ID: {}", correlationId);
                    break;
                }
            }
        }

        if (encryptedAccessToken == null || encryptedAccessToken.isEmpty()) {
            logger.error("[ProxyController:processLogoutRequest] No Access token found in cookies for send request, Correlation ID: {}", correlationId);
            return false;
        }

        try {
            // Add the encrypted refresh token to the headers
            headers.set("Authorization", "Bearer " + encryptedAccessToken);
            logger.info("[ProxyController:processLogoutRequest] Encrypted Access token added to headers for send request, Correlation ID: {}", correlationId);

            return true;
        } catch (Exception e) {
            logger.error("[ProxyController:processLogoutRequest] Error processing refresh token: {}, Correlation ID: {}",
                    e.getMessage(), correlationId);
            return false;
        }
    }
}

