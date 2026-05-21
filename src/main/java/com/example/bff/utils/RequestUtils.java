package com.example.bff.utils;

import org.springframework.stereotype.Component;

import java.util.UUID;
@Component
public class RequestUtils {

    public String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }
}
