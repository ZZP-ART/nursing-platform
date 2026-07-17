package com.nursing.operations.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CaregiverApplicationRequest {
    @NotBlank @Size(max=32) private String realName;
    @NotBlank @Size(max=20) private String phone;
    @NotBlank @Size(max=64) private String serviceDistrict;
    @NotBlank @Size(max=256) private String skills;
    public String getRealName(){return realName;} public void setRealName(String v){realName=v;} public String getPhone(){return phone;} public void setPhone(String v){phone=v;} public String getServiceDistrict(){return serviceDistrict;} public void setServiceDistrict(String v){serviceDistrict=v;} public String getSkills(){return skills;} public void setSkills(String v){skills=v;}
}
