package com.nursing.user.controller;

import com.nursing.common.result.Result;
import com.nursing.user.dto.request.AdminLoginRequest;
import com.nursing.user.dto.response.AuthResponse;
import com.nursing.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminAuthController {
    private final UserService users;
    public AdminAuthController(UserService users) { this.users = users; }
    @PostMapping("/api/v1/admin/login")
    public Result<AuthResponse> login(@Valid @RequestBody AdminLoginRequest request) { return Result.success(users.adminLogin(request)); }
}
