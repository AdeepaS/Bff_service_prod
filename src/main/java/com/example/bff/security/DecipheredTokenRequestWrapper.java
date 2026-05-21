package com.example.bff.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class DecipheredTokenRequestWrapper extends HttpServletRequestWrapper {

    private final String token;
    private static final Logger logger = LoggerFactory.getLogger(DecipheredTokenRequestWrapper.class);

    public DecipheredTokenRequestWrapper(HttpServletRequest request, String token) {
        super(request);
        logger.info("[DecipheredTokenRequestWrapper:DecipheredTokenRequestWrapper] Deciphered token (without Bearer): {} ", token);
        // token already decrypted; add Bearer prefix if needed
        this.token = "Bearer " + token;
        logger.info("[DecipheredTokenRequestWrapper:DecipheredTokenRequestWrapper] Deciphered token (with Bearer): {}", this.token);
    }

    @Override
    public String getHeader(String name) {
        if ("Authorization".equalsIgnoreCase(name)) {
            logger.info("[DecipheredTokenRequestWrapper:getHeader] Returning modified Authorization header");
            return token;
        }
        String headerValue = super.getHeader(name);
        logger.info("[DecipheredTokenRequestWrapper:getHeader] Returning original header '{}': '{}'", name, headerValue);
        return headerValue;
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        if ("Authorization".equalsIgnoreCase(name)) {
            logger.info("[DecipheredTokenRequestWrapper:getHeaders] Returning modified Authorization headers");
            return Collections.enumeration(Collections.singletonList(token));
        }
        Enumeration<String> originalHeaders = super.getHeaders(name);
        logger.info("[DecipheredTokenRequestWrapper:getHeaders] Returning original headers for '{}': {}", name, Collections.list(originalHeaders));
        return originalHeaders;
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        // Get the original header names
        List<String> headerNames = Collections.list(super.getHeaderNames());
        // Ensure Authorization header is included
        if (!headerNames.contains("Authorization")) {
            headerNames.add("Authorization");
            logger.info("[DecipheredTokenRequestWrapper:getHeaderNames] Added Authorization header to the list");
        }
        logger.info("[DecipheredTokenRequestWrapper:getHeaderNames] Returning header names: {}", headerNames);
        return Collections.enumeration(headerNames);
    }
}