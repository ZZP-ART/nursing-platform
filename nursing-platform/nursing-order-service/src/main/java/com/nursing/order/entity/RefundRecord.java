package com.nursing.order.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class RefundRecord {
    private Long id; private Long orderId; private Long paymentRecordId; private String refundNo; private BigDecimal refundAmount;
    private Integer status; private Integer retryCount; private String leaseOwner; private LocalDateTime leaseExpireTime;
    private LocalDateTime nextExecuteTime; private String providerRefundNo; private String failureReason;
    public Long getId() { return id; } public void setId(Long value) { id = value; }
    public Long getOrderId() { return orderId; } public void setOrderId(Long value) { orderId = value; }
    public Long getPaymentRecordId() { return paymentRecordId; } public void setPaymentRecordId(Long value) { paymentRecordId = value; }
    public String getRefundNo() { return refundNo; } public void setRefundNo(String value) { refundNo = value; }
    public BigDecimal getRefundAmount() { return refundAmount; } public void setRefundAmount(BigDecimal value) { refundAmount = value; }
    public Integer getStatus() { return status; } public void setStatus(Integer value) { status = value; }
    public Integer getRetryCount() { return retryCount; } public void setRetryCount(Integer value) { retryCount = value; }
    public String getLeaseOwner() { return leaseOwner; } public void setLeaseOwner(String value) { leaseOwner = value; }
    public LocalDateTime getLeaseExpireTime() { return leaseExpireTime; } public void setLeaseExpireTime(LocalDateTime value) { leaseExpireTime = value; }
    public LocalDateTime getNextExecuteTime() { return nextExecuteTime; } public void setNextExecuteTime(LocalDateTime value) { nextExecuteTime = value; }
    public String getProviderRefundNo() { return providerRefundNo; } public void setProviderRefundNo(String value) { providerRefundNo = value; }
    public String getFailureReason() { return failureReason; } public void setFailureReason(String value) { failureReason = value; }
}
