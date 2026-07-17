package com.nursing.user.controller;

import com.nursing.common.constant.ApiCode;
import com.nursing.common.exception.BusinessException;
import com.nursing.common.result.Result;
import com.nursing.user.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/users")
public class InternalUserController {
    private final UserService userService;
    private final String internalToken;

    public InternalUserController(UserService userService,
                                  @Value("${nursing.internal.token:}") String internalToken) {
        this.userService = userService;
        this.internalToken = internalToken;
    }

    @PostMapping("/{id}/roles/{roleCode}")
    public Result<Void> grantRole(@PathVariable("id") Long userId,
                                  @PathVariable String roleCode,
                                  @RequestHeader(value = "X-Internal-Token", required = false) String token) {
        if (!StringUtils.hasText(internalToken) || !internalToken.equals(token)) {
            throw new BusinessException(ApiCode.FORBIDDEN, "内部服务鉴权失败", HttpStatus.FORBIDDEN);
        }
        userService.grantRole(userId, roleCode);
        return Result.success();
    }
}
