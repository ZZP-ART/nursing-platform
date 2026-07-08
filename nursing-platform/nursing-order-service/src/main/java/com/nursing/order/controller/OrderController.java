package com.nursing.order.controller;

import com.nursing.common.constant.ApiCode;
import com.nursing.common.exception.BusinessException;
import com.nursing.common.result.Result;
import com.nursing.order.dto.response.PrepayTokenResponse;
import com.nursing.order.service.IdempotentService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
    private final IdempotentService idempotentService;

    public OrderController(IdempotentService idempotentService) {
        this.idempotentService = idempotentService;
    }

    @PostMapping("/prepay-token")
    public Result<PrepayTokenResponse> prepayToken(
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        requireUserId(userId);
        return Result.success(idempotentService.issuePrepayToken());
    }

    private Long requireUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(ApiCode.UNAUTHORIZED, "未登录或登录已过期");
        }
        return userId;
    }
}
