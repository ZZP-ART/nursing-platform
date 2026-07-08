package com.nursing.user.controller;

import com.nursing.common.result.Result;
import com.nursing.user.dto.request.LoginRequest;
import com.nursing.user.dto.request.RegisterRequest;
import com.nursing.user.dto.response.AuthResponse;
import com.nursing.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<Result<AuthResponse>> register(@RequestBody @Valid RegisterRequest request) {
        Result<AuthResponse> result = Result.success(userService.register(request));
        result.setMessage("注册成功");
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/login")
    public Result<AuthResponse> login(@RequestBody @Valid LoginRequest request) {
        Result<AuthResponse> result = Result.success(userService.login(request));
        result.setMessage("登录成功");
        return result;
    }

    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("Authorization") String authorizationHeader) {
        userService.logout(authorizationHeader);
        Result<Void> result = Result.success();
        result.setMessage("登出成功");
        return result;
    }
}
