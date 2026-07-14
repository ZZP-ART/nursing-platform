package com.nursing.user.entity;

import java.time.LocalDateTime;

public class SmsOutboxEvent {
    private Long id;
    private Long requestId;
    private String eventKey;
    private String eventType;
    private Integer status;
    private String leaseOwner;
    private LocalDateTime leaseExpireTime;
    private LocalDateTime nextExecuteTime;
    private Integer retryCount;
    private LocalDateTime processingTime;
    private String failureReason;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getRequestId() { return requestId; }
    public void setRequestId(Long requestId) { this.requestId = requestId; }
    public String getEventKey() { return eventKey; }
    public void setEventKey(String eventKey) { this.eventKey = eventKey; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getLeaseOwner() { return leaseOwner; }
    public void setLeaseOwner(String leaseOwner) { this.leaseOwner = leaseOwner; }
    public LocalDateTime getLeaseExpireTime() { return leaseExpireTime; }
    public void setLeaseExpireTime(LocalDateTime leaseExpireTime) { this.leaseExpireTime = leaseExpireTime; }
    public LocalDateTime getNextExecuteTime() { return nextExecuteTime; }
    public void setNextExecuteTime(LocalDateTime nextExecuteTime) { this.nextExecuteTime = nextExecuteTime; }
    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }
    public LocalDateTime getProcessingTime() { return processingTime; }
    public void setProcessingTime(LocalDateTime processingTime) { this.processingTime = processingTime; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
