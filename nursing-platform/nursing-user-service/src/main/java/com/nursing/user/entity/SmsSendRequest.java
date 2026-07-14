package com.nursing.user.entity;

import java.time.LocalDateTime;

public class SmsSendRequest {
    private Long id;
    private String idempotencyKey;
    private String requestFingerprint;
    private String phone;
    private String smsType;
    private String requestIp;
    private Integer status;
    private String responseSnapshot;
    private String failureReason;
    private LocalDateTime idempotentExpireTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getRequestFingerprint() { return requestFingerprint; }
    public void setRequestFingerprint(String requestFingerprint) { this.requestFingerprint = requestFingerprint; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getSmsType() { return smsType; }
    public void setSmsType(String smsType) { this.smsType = smsType; }
    public String getRequestIp() { return requestIp; }
    public void setRequestIp(String requestIp) { this.requestIp = requestIp; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getResponseSnapshot() { return responseSnapshot; }
    public void setResponseSnapshot(String responseSnapshot) { this.responseSnapshot = responseSnapshot; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public LocalDateTime getIdempotentExpireTime() { return idempotentExpireTime; }
    public void setIdempotentExpireTime(LocalDateTime idempotentExpireTime) { this.idempotentExpireTime = idempotentExpireTime; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
