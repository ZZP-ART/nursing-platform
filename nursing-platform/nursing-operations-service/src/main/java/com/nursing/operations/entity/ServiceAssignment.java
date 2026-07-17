package com.nursing.operations.entity;

import java.time.LocalDateTime;

public class ServiceAssignment {
    private Long id;
    private Long orderId;
    private Long activeOrderId;
    private Long merchantId;
    private Long caregiverUserId;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime acceptedTime;
    private LocalDateTime rejectedTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Long getActiveOrderId() { return activeOrderId; }
    public void setActiveOrderId(Long activeOrderId) { this.activeOrderId = activeOrderId; }
    public Long getMerchantId() { return merchantId; }
    public void setMerchantId(Long merchantId) { this.merchantId = merchantId; }
    public Long getCaregiverUserId() { return caregiverUserId; }
    public void setCaregiverUserId(Long caregiverUserId) { this.caregiverUserId = caregiverUserId; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getAcceptedTime() { return acceptedTime; }
    public void setAcceptedTime(LocalDateTime acceptedTime) { this.acceptedTime = acceptedTime; }
    public LocalDateTime getRejectedTime() { return rejectedTime; }
    public void setRejectedTime(LocalDateTime rejectedTime) { this.rejectedTime = rejectedTime; }
}
