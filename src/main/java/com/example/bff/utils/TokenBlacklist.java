package com.example.bff.utils;

import com.example.bff.logger.LoggerAdapter;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TokenBlacklist {

    private final LoggerAdapter logger;
    private final Map<String, Long> blacklist = new ConcurrentHashMap<>();

    public TokenBlacklist(LoggerAdapter logger) {
        this.logger = logger;
    }
    public void addToBlacklist(String token, long expirationTime) {
        blacklist.put(token, expirationTime);
        logger.info("[TokenBlacklist:addToBlacklist] Token added to blacklist: {} with expiration time: {}", token, expirationTime);
    }
    public boolean isTokenBlacklisted(String token) {
        logger.info("[TokenBlacklist:isTokenBlacklisted] Checking if token is blacklisted: {}", token);

        Long expirationTime = blacklist.get(token);
        if (expirationTime == null) {
            logger.info("[TokenBlacklist:isTokenBlacklisted] Token is not blacklisted: {}", token);
            return false;
        }

        if (System.currentTimeMillis() > expirationTime) {
            logger.info("[TokenBlacklist:isTokenBlacklisted] Token has expired and will be removed from the blacklist: {}", token);
            blacklist.remove(token);
            return false;
        }

        logger.info("[TokenBlacklist:isTokenBlacklisted] Token is blacklisted and still valid: {}", token);
        return true;
    }
    //
    public void cleanUpExpiredTokens() {
        long currentTime = System.currentTimeMillis();
        logger.info("[TokenBlacklist:cleanUpExpiredTokens] Starting cleanup of expired tokens at time: {}", currentTime);

        int initialSize = blacklist.size();
        blacklist.entrySet().removeIf(entry -> entry.getValue() <= currentTime);
        int finalSize = blacklist.size();

        logger.info("[TokenBlacklist:cleanUpExpiredTokens] Cleanup completed. Removed {} expired tokens. Current blacklist size: {}",
                (initialSize - finalSize), finalSize);
    }
}
