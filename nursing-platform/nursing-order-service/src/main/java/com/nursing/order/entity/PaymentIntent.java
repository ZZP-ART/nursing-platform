package com.nursing.order.entity;

import java.time.LocalDateTime;

public class PaymentIntent {
    private Long id;
    private Long orderId;
    private Long userId;
    private String idempotentKey;
    private String requestHash;
    private String payChannel;
    private Integer status;
    private String responseSnapshot;
    private Long paymentRecordId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getOrderId() { return orderId; } public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Long getUserId() { return userId; } public void setUserId(Long userId) { this.userId = userId; }
    public String getIdempotentKey() { return idempotentKey; } public void setIdempotentKey(String value) { this.idempotentKey = value; }
    public String getRequestHash() { return requestHash; } public void setRequestHash(String value) { this.requestHash = value; }
    public String getPayChannel() { return payChannel; } public void setPayChannel(String value) { this.payChannel = value; }
    public Integer getStatus() { return status; } public void setStatus(Integer value) { this.status = value; }
    public String getResponseSnapshot() { return responseSnapshot; } public void setResponseSnapshot(String value) { this.responseSnapshot = value; }
    public Long getPaymentRecordId() { return paymentRecordId; } public void setPaymentRecordId(Long value) { this.paymentRecordId = value; }
    public LocalDateTime getCreateTime() { return createTime; } public void setCreateTime(LocalDateTime value) { this.createTime = value; }
    public LocalDateTime getUpdateTime() { return updateTime; } public void setUpdateTime(LocalDateTime value) { this.updateTime = value; }
}
