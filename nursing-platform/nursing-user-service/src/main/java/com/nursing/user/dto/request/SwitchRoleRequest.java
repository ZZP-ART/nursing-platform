package com.nursing.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class SwitchRoleRequest {
    @NotBlank
    @Pattern(regexp = "^(CUSTOMER|CAREGIVER|MERCHANT_MEMBER|ADMIN)$")
    private String targetRole;
    public String getTargetRole() { return targetRole; }
    public void setTargetRole(String targetRole) { this.targetRole = targetRole; }
}
