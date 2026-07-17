package com.nursing.operations.dto.request;

import jakarta.validation.constraints.Size;

public class AssignmentRemarkRequest {
    @Size(max = 256)
    private String remark;

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
