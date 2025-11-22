package com.hc.authservice.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleRateLimitExceeded(
            RateLimitExceededException ex) {

        log.warn(" Service-level rate limit exceeded: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .header("X-Rate-Limit-Retry-After-Seconds",
                        String.valueOf(ex.getRetryAfterSeconds()))
                .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
                .body(Map.of(
                        "error", "rate_limit_exceeded",
                        "message", ex.getMessage(),
                        "retryAfter", ex.getRetryAfterSeconds(),
                        "level", ex.getLimitType()
                ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(
            IllegalArgumentException ex) {

        log.warn("️ Bad request: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "error", "bad_request",
                        "message", ex.getMessage()
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception ex) {
        log.error(" Unexpected error: ", ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "error", "internal_server_error",
                        "message", "An unexpected error occurred"
                ));
    }
}