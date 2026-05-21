package com.example.bff.DTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WebSocketMessage {
    
    @JsonProperty("type")
    private String type;
    
    @JsonProperty("token")
    private String token;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("code")
    private String code;
    
    @JsonProperty("userId")
    private Integer userId;
    
    @JsonProperty("sessionId")
    private String sessionId;
    
    @JsonProperty("timestamp")
    private Long timestamp;

    // Default constructor
    public WebSocketMessage() {
    }

    // Constructor for error/success responses
    public WebSocketMessage(String type, String message, String code, Long timestamp) {
        this.type = type;
        this.message = message;
        this.code = code;
        this.timestamp = timestamp;
    }

    // Static factory methods for common responses
    public static WebSocketMessage authSuccess(String sessionId, Integer userId) {
        WebSocketMessage msg = new WebSocketMessage();
        msg.setType("auth_success");
        msg.setMessage("Authentication successful");
        msg.setSessionId(sessionId);
        msg.setUserId(userId);
        msg.setTimestamp(System.currentTimeMillis());
        return msg;
    }

    public static WebSocketMessage authError(String message, String code) {
        WebSocketMessage msg = new WebSocketMessage();
        msg.setType("auth_error");
        msg.setMessage(message);
        msg.setCode(code);
        msg.setTimestamp(System.currentTimeMillis());
        return msg;
    }

    public static WebSocketMessage authTimeout() {
        return authError("Authentication timeout. Please send auth message within 10 seconds.", "AUTH_TIMEOUT");
    }

    // Getters and Setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}
