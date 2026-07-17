package com.nursing.operations.entity;

import java.time.LocalDateTime;

public class CaregiverApplication {
    private Long id; private Long userId; private String realName; private String phone; private String serviceDistrict; private String skills; private Integer status; private String reviewRemark; private LocalDateTime createTime; private LocalDateTime updateTime;
    public Long getId(){return id;} public void setId(Long v){id=v;} public Long getUserId(){return userId;} public void setUserId(Long v){userId=v;} public String getRealName(){return realName;} public void setRealName(String v){realName=v;} public String getPhone(){return phone;} public void setPhone(String v){phone=v;} public String getServiceDistrict(){return serviceDistrict;} public void setServiceDistrict(String v){serviceDistrict=v;} public String getSkills(){return skills;} public void setSkills(String v){skills=v;} public Integer getStatus(){return status;} public void setStatus(Integer v){status=v;} public String getReviewRemark(){return reviewRemark;} public void setReviewRemark(String v){reviewRemark=v;} public LocalDateTime getCreateTime(){return createTime;} public void setCreateTime(LocalDateTime v){createTime=v;} public LocalDateTime getUpdateTime(){return updateTime;} public void setUpdateTime(LocalDateTime v){updateTime=v;}
}
