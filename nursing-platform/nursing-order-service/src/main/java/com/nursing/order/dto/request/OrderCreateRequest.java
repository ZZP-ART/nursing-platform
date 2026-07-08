package com.nursing.order.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class OrderCreateRequest {
    @NotNull(message = "服务项目ID不能为空")
    @Positive(message = "服务项目ID必须大于0")
    private Long serviceItemId;

    @NotNull(message = "服务规格ID不能为空")
    @Positive(message = "服务规格ID必须大于0")
    private Long serviceSpecId;

    @NotNull(message = "地址ID不能为空")
    @Positive(message = "地址ID必须大于0")
    private Long addressId;

    @NotNull(message = "预约日期不能为空")
    private LocalDate serviceDate;

    @NotBlank(message = "预约时段不能为空")
    @Pattern(regexp = "MORNING|AFTERNOON|EVENING", message = "预约时段不合法")
    private String serviceTimeSlot;

    @Size(max = 200, message = "备注最多200个字符")
    private String remark;

    public Long getServiceItemId() { return serviceItemId; }
    public void setServiceItemId(Long serviceItemId) { this.serviceItemId = serviceItemId; }
    public Long getServiceSpecId() { return serviceSpecId; }
    public void setServiceSpecId(Long serviceSpecId) { this.serviceSpecId = serviceSpecId; }
    public Long getAddressId() { return addressId; }
    public void setAddressId(Long addressId) { this.addressId = addressId; }
    public LocalDate getServiceDate() { return serviceDate; }
    public void setServiceDate(LocalDate serviceDate) { this.serviceDate = serviceDate; }
    public String getServiceTimeSlot() { return serviceTimeSlot; }
    public void setServiceTimeSlot(String serviceTimeSlot) { this.serviceTimeSlot = serviceTimeSlot; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
