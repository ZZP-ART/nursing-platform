package com.nursing.operations.entity;

import java.time.LocalDateTime;

public class ServiceAction {
    private String action;
    private String remark;
    private LocalDateTime createTime;

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
