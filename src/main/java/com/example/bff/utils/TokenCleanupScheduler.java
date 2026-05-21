package com.example.bff.utils;

import com.example.bff.logger.LoggerAdapter;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TokenCleanupScheduler {

    private final TokenBlacklist tokenBlacklist;
    private final LoggerAdapter logger;

    public TokenCleanupScheduler(TokenBlacklist tokenBlacklist, LoggerAdapter logger) {
        this.tokenBlacklist = tokenBlacklist;
        this.logger = logger;
    }

    @Scheduled(fixedRate = 3600000) // Every 1 hour
    public void cleanUpBlacklistedTokens() {
        logger.info("[TokenCleanupScheduler:cleanUpBlacklistedTokens] Scheduled cleanup task started.");

        try {
            tokenBlacklist.cleanUpExpiredTokens();
            logger.info("[TokenCleanupScheduler:cleanUpBlacklistedTokens] Cleanup task completed successfully.");
        } catch (Exception e) {
            logger.error("[TokenCleanupScheduler:cleanUpBlacklistedTokens] Error occurred during cleanup task: {}", e.getMessage(), e);
        }
    }
}
