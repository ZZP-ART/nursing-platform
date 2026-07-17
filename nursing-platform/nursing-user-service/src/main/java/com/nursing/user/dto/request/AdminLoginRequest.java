package com.nursing.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AdminLoginRequest {
    @NotBlank @Size(max = 20)
    private String username;
    @NotBlank @Size(min = 6, max = 72)
    private String password;
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
