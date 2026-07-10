package com.nursing.user.exception;

import org.springframework.http.HttpStatus;

public class SmsRateLimitException extends UserBusinessException {

    private final long retryAfterSeconds;

    public SmsRateLimitException(HttpStatus status, int code, String message, long retryAfterSeconds) {
        super(status, code, message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
