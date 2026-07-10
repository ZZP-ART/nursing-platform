package com.nursing.user.exception;

import com.nursing.common.result.Result;
import com.nursing.user.constant.UserErrorCode;
import com.nursing.user.dto.response.SmsCodeResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class UserExceptionHandlerTest {

    @Test
    void smsRateLimitResponseContainsRetryAfterSeconds() {
        UserExceptionHandler handler = new UserExceptionHandler();

        ResponseEntity<Result<SmsCodeResponse>> response = handler.handleSmsRateLimitException(
                new SmsRateLimitException(
                        HttpStatus.TOO_MANY_REQUESTS,
                        UserErrorCode.SMS_SEND_TOO_FREQUENT,
                        "发送过于频繁，请稍后重试",
                        37L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(UserErrorCode.SMS_SEND_TOO_FREQUENT);
        assertThat(response.getBody().getData().getRetryAfterSeconds()).isEqualTo(37L);
    }
}
