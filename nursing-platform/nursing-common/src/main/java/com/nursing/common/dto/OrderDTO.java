package com.nursing.common.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class OrderDTO implements Serializable {
    private Long orderId;
    private String orderNo;
    private Integer status;
    private Long serviceItemId;
    private String serviceItemName;
    private String specName;
    private BigDecimal totalAmount;
    private Long userId;
    private Long merchantId;
    private Long serviceSpecId;
    private String receiverName;
    private String receiverPhone;
    private String addressDetail;
    private String remark;
    private LocalDate serviceDate;
    private String serviceTimeSlot;
    private LocalDateTime createTime;

    public OrderDTO() {}

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Long getServiceItemId() { return serviceItemId; }
    public void setServiceItemId(Long serviceItemId) { this.serviceItemId = serviceItemId; }
    public String getServiceItemName() { return serviceItemName; }
    public void setServiceItemName(String serviceItemName) { this.serviceItemName = serviceItemName; }
    public String getSpecName() { return specName; }
    public void setSpecName(String specName) { this.specName = specName; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getMerchantId() { return merchantId; }
    public void setMerchantId(Long merchantId) { this.merchantId = merchantId; }
    public Long getServiceSpecId() { return serviceSpecId; }
    public void setServiceSpecId(Long serviceSpecId) { this.serviceSpecId = serviceSpecId; }
    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String receiverName) { this.receiverName = receiverName; }
    public String getReceiverPhone() { return receiverPhone; }
    public void setReceiverPhone(String receiverPhone) { this.receiverPhone = receiverPhone; }
    public String getAddressDetail() { return addressDetail; }
    public void setAddressDetail(String addressDetail) { this.addressDetail = addressDetail; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDate getServiceDate() { return serviceDate; }
    public void setServiceDate(LocalDate serviceDate) { this.serviceDate = serviceDate; }
    public String getServiceTimeSlot() { return serviceTimeSlot; }
    public void setServiceTimeSlot(String serviceTimeSlot) { this.serviceTimeSlot = serviceTimeSlot; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
