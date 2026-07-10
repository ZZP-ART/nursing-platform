package com.nursing.user.dto.response;

public class SmsCodeResponse {

    private long expireSeconds;
    private Long retryAfterSeconds;

    public SmsCodeResponse() {
    }

    public SmsCodeResponse(long expireSeconds) {
        this.expireSeconds = expireSeconds;
    }

    public long getExpireSeconds() {
        return expireSeconds;
    }

    public void setExpireSeconds(long expireSeconds) {
        this.expireSeconds = expireSeconds;
    }

    public Long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    public void setRetryAfterSeconds(Long retryAfterSeconds) {
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
