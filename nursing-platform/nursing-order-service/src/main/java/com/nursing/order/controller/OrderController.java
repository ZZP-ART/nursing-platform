package com.nursing.order.controller;

import com.nursing.common.constant.ApiCode;
import com.nursing.common.exception.BusinessException;
import com.nursing.common.result.PageResult;
import com.nursing.common.result.Result;
import com.nursing.order.dto.request.CancelOrderRequest;
import com.nursing.order.dto.request.OrderCreateRequest;
import com.nursing.order.dto.request.OrderPageQuery;
import com.nursing.order.dto.response.CancelResponse;
import com.nursing.order.dto.response.OrderCreateResponse;
import com.nursing.order.dto.response.OrderDetailResponse;
import com.nursing.order.dto.response.OrderListResponse;
import com.nursing.order.dto.response.PrepayTokenResponse;
import com.nursing.order.service.IOrderService;
import com.nursing.order.service.IdempotentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
    private final IdempotentService idempotentService;
    private final IOrderService orderService;

    public OrderController(IdempotentService idempotentService, IOrderService orderService) {
        this.idempotentService = idempotentService;
        this.orderService = orderService;
    }

    @PostMapping("/prepay-token")
    public Result<PrepayTokenResponse> prepayToken(
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        requireUserId(userId);
        return Result.success(idempotentService.issuePrepayToken());
    }

    @PostMapping
    public Result<OrderCreateResponse> create(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "Idempotent-Key", required = false) String idempotentKey,
            @Valid @RequestBody OrderCreateRequest request) {
        return Result.success(orderService.createOrder(requireUserId(userId), idempotentKey, request));
    }

    @GetMapping
    public Result<PageResult<OrderListResponse>> list(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @Valid OrderPageQuery query) {
        return Result.success(orderService.listOrders(requireUserId(userId), query));
    }

    @GetMapping("/{id}")
    public Result<OrderDetailResponse> detail(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable("id") @Positive(message = "订单ID必须大于0") Long id) {
        return Result.success(orderService.getOrderDetail(requireUserId(userId), id));
    }

    @PostMapping("/{id}/cancel")
    public Result<CancelResponse> cancel(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable("id") @Positive(message = "订单ID必须大于0") Long id,
            @Valid @RequestBody(required = false) CancelOrderRequest request) {
        return Result.success(orderService.cancelOrder(requireUserId(userId), id, request));
    }

    private Long requireUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(ApiCode.UNAUTHORIZED, "未登录或登录已过期");
        }
        return userId;
    }
}
