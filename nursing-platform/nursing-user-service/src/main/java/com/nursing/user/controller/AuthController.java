package com.nursing.user.controller;

import com.nursing.common.constant.ApiCode;
import com.nursing.common.exception.BusinessException;
import com.nursing.common.result.Result;
import com.nursing.user.dto.request.SwitchRoleRequest;
import com.nursing.user.dto.response.AuthResponse;
import com.nursing.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final UserService users;
    private final String gatewayToken;
    public AuthController(UserService users, @Value("${nursing.gateway.trusted-token:}") String gatewayToken) { this.users = users; this.gatewayToken = gatewayToken; }
    @PostMapping("/switch-role")
    public Result<AuthResponse> switchRole(@RequestHeader(value = "X-User-Id", required = false) Long userId,
                                           @RequestHeader(value = "X-Gateway-Token", required = false) String token,
                                           @Valid @RequestBody SwitchRoleRequest request) {
        if (userId == null || userId <= 0) throw new BusinessException(ApiCode.UNAUTHORIZED, "Unauthorized", HttpStatus.UNAUTHORIZED);
        if (!StringUtils.hasText(gatewayToken) || !gatewayToken.equals(token)) throw new BusinessException(ApiCode.FORBIDDEN, "Forbidden", HttpStatus.FORBIDDEN);
        return Result.success(users.switchRole(userId, request.getTargetRole()));
    }
}
