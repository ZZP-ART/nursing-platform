package com.nursing.user.exception;

import com.nursing.common.result.Result;
import com.nursing.user.dto.response.SmsCodeResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.nursing.user")
public class UserExceptionHandler {

    @ExceptionHandler(SmsRateLimitException.class)
    public ResponseEntity<Result<SmsCodeResponse>> handleSmsRateLimitException(SmsRateLimitException exception) {
        SmsCodeResponse response = new SmsCodeResponse();
        response.setRetryAfterSeconds(exception.getRetryAfterSeconds());
        Result<SmsCodeResponse> result = Result.error(exception.getCode(), exception.getMessage());
        result.setData(response);
        return ResponseEntity
                .status(exception.getStatus())
                .body(result);
    }

    @ExceptionHandler(UserBusinessException.class)
    public ResponseEntity<Result<Void>> handleUserBusinessException(UserBusinessException exception) {
        return ResponseEntity
                .status(exception.getStatus())
                .body(Result.error(exception.getCode(), exception.getMessage()));
    }
}
