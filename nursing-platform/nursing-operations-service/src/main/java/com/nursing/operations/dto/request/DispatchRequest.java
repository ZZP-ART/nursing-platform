package com.nursing.operations.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class DispatchRequest {
    @NotNull @Positive
    private Long caregiverUserId;
    @Size(max = 256)
    private String remark;

    public Long getCaregiverUserId() { return caregiverUserId; }
    public void setCaregiverUserId(Long caregiverUserId) { this.caregiverUserId = caregiverUserId; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
