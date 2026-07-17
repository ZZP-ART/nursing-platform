package com.nursing.user.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public class AuthResponse {

    private String token;
    private LocalDateTime expireTime;
    private UserInfoResponse user;
    private List<String> roles;
    private String currentRole;
    private List<String> permissions;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public LocalDateTime getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(LocalDateTime expireTime) {
        this.expireTime = expireTime;
    }

    public UserInfoResponse getUser() {
        return user;
    }

    public void setUser(UserInfoResponse user) {
        this.user = user;
    }
    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) { this.roles = roles; }
    public String getCurrentRole() { return currentRole; }
    public void setCurrentRole(String currentRole) { this.currentRole = currentRole; }
    public List<String> getPermissions() { return permissions; }
    public void setPermissions(List<String> permissions) { this.permissions = permissions; }
}
