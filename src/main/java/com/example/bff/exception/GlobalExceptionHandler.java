package com.example.bff.exception;
import com.example.bff.DTO.ApiResponse;
import com.example.bff.logger.LoggerAdapter;
import com.example.bff.error.BffErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.nio.file.AccessDeniedException;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

   private final LoggerAdapter logger;

    public GlobalExceptionHandler(LoggerAdapter logger) {
        this.logger = logger;
    }

    // Handle HTTP Client errors (e.g., 4xx responses)
    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<ApiResponse<Object>> handleHttpClientError(HttpClientErrorException e) {
        logger.error("Client error: {}", e.getMessage());
        ApiResponse<Object> body = new ApiResponse<>(false, e.getStatusCode().value(), e.getMessage(), null);
        body.setErrorCode(BffErrorCode.BFF_P2_BACKEND_CLIENT_ERROR.code());
        return new ResponseEntity<>(body, e.getStatusCode());
    }

    // Handle HTTP Server errors (e.g., 5xx responses)
    @ExceptionHandler(HttpServerErrorException.class)
    public ResponseEntity<ApiResponse<Object>> handleHttpServerError(HttpServerErrorException e) {
        logger.error("Server error: {}", e.getMessage());
        ApiResponse<Object> body = new ApiResponse<>(false, e.getStatusCode().value(), e.getMessage(), null);
        body.setErrorCode(BffErrorCode.BFF_P2_BACKEND_SERVER_ERROR.code());
        return new ResponseEntity<>(body, e.getStatusCode());
    }

    // Handle validation errors (e.g., @Validated)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationException(MethodArgumentNotValidException e) {
        String errorMessage = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        logger.error("Validation error: {}", errorMessage);
        ApiResponse<Object> body = new ApiResponse<>(false, HttpStatus.BAD_REQUEST.value(), errorMessage, null);
        body.setErrorCode(BffErrorCode.BFF_P1_UNCLASSIFIED_FAILURE.code());
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    // Handle JSON parse errors
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleJsonParseError(HttpMessageNotReadableException e) {
        logger.error("Malformed JSON request: {}", e.getMessage());
        ApiResponse<Object> body = new ApiResponse<>(false, HttpStatus.BAD_REQUEST.value(), "Malformed JSON request", null);
        body.setErrorCode(BffErrorCode.BFF_P1_UNCLASSIFIED_FAILURE.code());
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    // Handle missing request parameters
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Object>> handleMissingParameter(MissingServletRequestParameterException e) {
        logger.error("Missing parameter: {}", e.getParameterName());
        ApiResponse<Object> body = new ApiResponse<>(false, HttpStatus.BAD_REQUEST.value(), "Missing parameter: " + e.getParameterName(), null);
        body.setErrorCode(BffErrorCode.BFF_P1_UNCLASSIFIED_FAILURE.code());
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    // Handle 404 errors (No handler found)
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNoHandlerFound(NoHandlerFoundException e) {
        logger.error("No handler found for URL: {}", e.getRequestURL());
        ApiResponse<Object> body = new ApiResponse<>(false, HttpStatus.NOT_FOUND.value(), "No handler found for the requested URL", null);
        body.setErrorCode(BffErrorCode.BFF_P1_UNCLASSIFIED_FAILURE.code());
        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }

    // Handle access denied errors
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDenied(AccessDeniedException e) {
        logger.error("Access denied: {}", e.getMessage());
        ApiResponse<Object> body = new ApiResponse<>(false, HttpStatus.FORBIDDEN.value(), "Access denied", null);
        body.setErrorCode(BffErrorCode.BFF_P1_ACCESS_DENIED.code());
        return new ResponseEntity<>(body, HttpStatus.FORBIDDEN);
    }

    // Handle illegal argument errors
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgument(IllegalArgumentException e) {
        logger.error("Illegal argument: {}", e.getMessage());
        ApiResponse<Object> body = new ApiResponse<>(false, HttpStatus.BAD_REQUEST.value(), e.getMessage(), null);
        body.setErrorCode(BffErrorCode.BFF_P1_UNCLASSIFIED_FAILURE.code());
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

   // Handle async request timeout errors
    @ExceptionHandler(AsyncRequestTimeoutException.class)
    public ResponseEntity<ApiResponse<Object>> handleAsyncTimeout(AsyncRequestTimeoutException e) {
        logger.error("Async request timeout: {}", e.getMessage());
        ApiResponse<Object> body = new ApiResponse<>(false, HttpStatus.REQUEST_TIMEOUT.value(), "Request timeout", null);
        body.setErrorCode(BffErrorCode.BFF_P1_UNCLASSIFIED_FAILURE.code());
        return new ResponseEntity<>(body, HttpStatus.REQUEST_TIMEOUT);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGeneralException(Exception e) {
        logger.error("An error occurred: {}", e.getMessage());
        ApiResponse<Object> body = new ApiResponse<>(false, HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal server error", null);
        body.setErrorCode(BffErrorCode.BFF_P1_UNCLASSIFIED_FAILURE.code());
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDeniedException(org.springframework.security.access.AccessDeniedException e) {
        logger.error("Access denied: {}", e.getMessage());
        ApiResponse<Object> body = new ApiResponse<>(true, HttpStatus.FORBIDDEN.value(), "Access denied: You don't have permission to access this resource", null);
        body.setErrorCode(BffErrorCode.BFF_P1_ACCESS_DENIED.code());
        return new ResponseEntity<>(body, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(TokenValidationException.class)
    public ResponseEntity<ApiResponse<Object>> handleTokenValidationException(TokenValidationException e) {
        logger.error("Token validation error: {}", e.getMessage());
        ApiResponse<Object> body = new ApiResponse<>(false, HttpStatus.UNAUTHORIZED.value(), "Invalid token", null);
        body.setErrorCode(BffErrorCode.BFF_P1_TOKEN_INVALID.code());
        return new ResponseEntity<>(body, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<ApiResponse<Object>> handleTokenExpiredException(TokenExpiredException e) {
        logger.error("Token expired: {}", e.getMessage());
        ApiResponse<Object> body = new ApiResponse<>(false, HttpStatus.UNAUTHORIZED.value(), "Token expired", null);
        body.setErrorCode(BffErrorCode.BFF_P1_TOKEN_EXPIRED.code());
        return new ResponseEntity<>(body, HttpStatus.UNAUTHORIZED);
    }

    public class TokenValidationException extends RuntimeException {
        public TokenValidationException(String message) {
            super(message);
        }

        public TokenValidationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public class TokenExpiredException extends TokenValidationException {
        public TokenExpiredException(String message) {
            super(message);
        }
    }

}

