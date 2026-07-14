package com.nursing.user.entity;

import java.time.LocalDateTime;

public class SmsRecordStatusTransition {
    private Long id;
    private Long smsRecordId;
    private Integer fromStatus;
    private Integer toStatus;
    private String transitionReason;
    private String providerReceipt;
    private LocalDateTime transitionTime;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSmsRecordId() { return smsRecordId; }
    public void setSmsRecordId(Long smsRecordId) { this.smsRecordId = smsRecordId; }
    public Integer getFromStatus() { return fromStatus; }
    public void setFromStatus(Integer fromStatus) { this.fromStatus = fromStatus; }
    public Integer getToStatus() { return toStatus; }
    public void setToStatus(Integer toStatus) { this.toStatus = toStatus; }
    public String getTransitionReason() { return transitionReason; }
    public void setTransitionReason(String transitionReason) { this.transitionReason = transitionReason; }
    public String getProviderReceipt() { return providerReceipt; }
    public void setProviderReceipt(String providerReceipt) { this.providerReceipt = providerReceipt; }
    public LocalDateTime getTransitionTime() { return transitionTime; }
    public void setTransitionTime(LocalDateTime transitionTime) { this.transitionTime = transitionTime; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
