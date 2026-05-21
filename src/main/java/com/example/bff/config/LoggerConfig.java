package com.example.bff.config;


import com.example.bff.logger.LoggerAdapter;
import com.example.bff.logger.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LoggerConfig {

    private final LoggerFactory loggerFactory;

    public LoggerConfig(LoggerFactory loggerFactory) {
        this.loggerFactory = loggerFactory;
    }

    @Bean
    public LoggerAdapter loggerAdapter() {
        return loggerFactory.createLoggerAdapter();
    }
}
