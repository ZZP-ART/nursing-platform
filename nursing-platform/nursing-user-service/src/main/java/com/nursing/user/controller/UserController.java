package com.nursing.user.controller;

import com.nursing.common.result.Result;
import com.nursing.user.dto.request.LoginRequest;
import com.nursing.user.dto.request.RegisterRequest;
import com.nursing.user.dto.request.ResetPasswordRequest;
import com.nursing.user.dto.request.UpdateUserProfileRequest;
import com.nursing.user.dto.response.AuthResponse;
import com.nursing.user.dto.response.ProfileUpdateResponse;
import com.nursing.user.dto.response.UserInfoResponse;
import com.nursing.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestAttribute;
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

    @PostMapping("/password/reset")
    public Result<Void> resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
        userService.resetPassword(request);
        Result<Void> result = Result.success();
        result.setMessage("密码重置成功");
        return result;
    }

    @GetMapping("/profile")
    public Result<UserInfoResponse> getProfile(@RequestAttribute("userId") Long userId) {
        return Result.success(userService.getProfile(userId));
    }

    @PatchMapping("/profile")
    public Result<ProfileUpdateResponse> updateProfile(@RequestAttribute("userId") Long userId,
                                                       @RequestBody @Valid UpdateUserProfileRequest request) {
        Result<ProfileUpdateResponse> result = Result.success(userService.updateProfile(userId, request));
        result.setMessage("修改成功");
        return result;
    }
}
