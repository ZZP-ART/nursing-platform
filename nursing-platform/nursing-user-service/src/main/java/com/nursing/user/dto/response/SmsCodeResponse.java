package com.nursing.user.dto.response;

public class SmsCodeResponse {

    private long expireSeconds;

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
}
