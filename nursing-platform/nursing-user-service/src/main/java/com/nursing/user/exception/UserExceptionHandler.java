package com.nursing.user.exception;

import com.nursing.common.result.Result;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.nursing.user")
public class UserExceptionHandler {

    @ExceptionHandler(UserBusinessException.class)
    public ResponseEntity<Result<Void>> handleUserBusinessException(UserBusinessException exception) {
        return ResponseEntity
                .status(exception.getStatus())
                .body(Result.error(exception.getCode(), exception.getMessage()));
    }
}
