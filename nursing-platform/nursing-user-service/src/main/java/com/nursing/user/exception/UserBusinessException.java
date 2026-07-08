package com.nursing.user.exception;

import org.springframework.http.HttpStatus;

public class UserBusinessException extends RuntimeException {

    private final int code;
    private final HttpStatus status;

    public UserBusinessException(HttpStatus status, int code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
