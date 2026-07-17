package com.nursing.operations.entity;

public class CaregiverProfile {
    private Long caregiverId;
    private Long userId;
    private String realName;
    private String serviceAreas;
    private String skills;
    private Integer auditStatus;
    private Integer maxDailyOrders;
    private Double rating;
    private Integer completedOrders;
    private String status;

    public Long getCaregiverId() { return caregiverId; }
    public void setCaregiverId(Long caregiverId) { this.caregiverId = caregiverId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }
    public String getServiceAreas() { return serviceAreas; }
    public void setServiceAreas(String serviceAreas) { this.serviceAreas = serviceAreas; }
    public String getSkills() { return skills; }
    public void setSkills(String skills) { this.skills = skills; }
    public Integer getAuditStatus() { return auditStatus; }
    public void setAuditStatus(Integer auditStatus) { this.auditStatus = auditStatus; }
    public Integer getMaxDailyOrders() { return maxDailyOrders; }
    public void setMaxDailyOrders(Integer maxDailyOrders) { this.maxDailyOrders = maxDailyOrders; }
    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }
    public Integer getCompletedOrders() { return completedOrders; }
    public void setCompletedOrders(Integer completedOrders) { this.completedOrders = completedOrders; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
