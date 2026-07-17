package com.nursing.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class LoginRequest {

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "登录方式不能为空")
    @Pattern(regexp = "^(password|sms)$", message = "登录方式不正确")
    private String loginMode;

    private String password;

    @Pattern(regexp = "^\\d{6}$", message = "验证码格式不正确")
    private String smsCode;

    @Pattern(regexp = "^(CUSTOMER|CAREGIVER|MERCHANT_MEMBER|ADMIN)$", message = "targetRole is invalid")
    private String targetRole;

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getLoginMode() {
        return loginMode;
    }

    public void setLoginMode(String loginMode) {
        this.loginMode = loginMode;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSmsCode() {
        return smsCode;
    }

    public void setSmsCode(String smsCode) {
        this.smsCode = smsCode;
    }

    public String getTargetRole() { return targetRole; }
    public void setTargetRole(String targetRole) { this.targetRole = targetRole; }
}
