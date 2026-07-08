package com.nursing.order.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class OrderHeader {
    private Long id;
    private String orderNo;
    private Long userId;
    private Integer source;
    private Integer version;
    private Long serviceItemId;
    private Long serviceSpecId;
    private String serviceItemName;
    private String specName;
    private BigDecimal specPrice;
    private Integer specDuration;
    private Long addressId;
    private String receiverName;
    private String receiverPhone;
    private String addressDetail;
    private LocalDate serviceDate;
    private String serviceTimeSlot;
    private BigDecimal totalAmount;
    private Integer status;
    private String remark;
    private String cancelReason;
    private Integer isDeleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Integer getSource() { return source; }
    public void setSource(Integer source) { this.source = source; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public Long getServiceItemId() { return serviceItemId; }
    public void setServiceItemId(Long serviceItemId) { this.serviceItemId = serviceItemId; }
    public Long getServiceSpecId() { return serviceSpecId; }
    public void setServiceSpecId(Long serviceSpecId) { this.serviceSpecId = serviceSpecId; }
    public String getServiceItemName() { return serviceItemName; }
    public void setServiceItemName(String serviceItemName) { this.serviceItemName = serviceItemName; }
    public String getSpecName() { return specName; }
    public void setSpecName(String specName) { this.specName = specName; }
    public BigDecimal getSpecPrice() { return specPrice; }
    public void setSpecPrice(BigDecimal specPrice) { this.specPrice = specPrice; }
    public Integer getSpecDuration() { return specDuration; }
    public void setSpecDuration(Integer specDuration) { this.specDuration = specDuration; }
    public Long getAddressId() { return addressId; }
    public void setAddressId(Long addressId) { this.addressId = addressId; }
    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String receiverName) { this.receiverName = receiverName; }
    public String getReceiverPhone() { return receiverPhone; }
    public void setReceiverPhone(String receiverPhone) { this.receiverPhone = receiverPhone; }
    public String getAddressDetail() { return addressDetail; }
    public void setAddressDetail(String addressDetail) { this.addressDetail = addressDetail; }
    public LocalDate getServiceDate() { return serviceDate; }
    public void setServiceDate(LocalDate serviceDate) { this.serviceDate = serviceDate; }
    public String getServiceTimeSlot() { return serviceTimeSlot; }
    public void setServiceTimeSlot(String serviceTimeSlot) { this.serviceTimeSlot = serviceTimeSlot; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }
    public Integer getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Integer isDeleted) { this.isDeleted = isDeleted; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
