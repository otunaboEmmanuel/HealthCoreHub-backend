package com.hc.authservice.exception;

import lombok.Getter;

@Getter
public class RateLimitExceededException extends RuntimeException {
    private final long retryAfterSeconds;
    private final String limitType;

    public RateLimitExceededException(String message, long retryAfterSeconds) {
        this(message, retryAfterSeconds, "service");
    }

    public RateLimitExceededException(String message, long retryAfterSeconds, String limitType) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
        this.limitType = limitType;
    }
}