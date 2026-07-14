package com.nursing.user.dto.response;

public class SmsCodeResponse {

    private long expireSeconds;
    private Long retryAfterSeconds;
    private String requestId;
    private String status;

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

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
