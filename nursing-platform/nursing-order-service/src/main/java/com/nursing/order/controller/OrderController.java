package com.nursing.order.controller;

import com.nursing.common.constant.ApiCode;
import com.nursing.common.exception.BusinessException;
import com.nursing.common.result.PageResult;
import com.nursing.common.result.Result;
import com.nursing.order.dto.request.CancelOrderRequest;
import com.nursing.order.dto.request.OrderCreateRequest;
import com.nursing.order.dto.request.OrderPageQuery;
import com.nursing.order.dto.request.PayRequest;
import com.nursing.order.dto.response.CancelResponse;
import com.nursing.order.dto.response.OrderCreateResponse;
import com.nursing.order.dto.response.OrderDetailResponse;
import com.nursing.order.dto.response.OrderListResponse;
import com.nursing.order.dto.response.PayResponse;
import com.nursing.order.dto.response.PrepayTokenResponse;
import com.nursing.order.service.IOrderService;
import com.nursing.order.service.IdempotentService;
import com.nursing.order.service.PaymentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Validated
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
    private final IdempotentService idempotentService;
    private final IOrderService orderService;
    private final PaymentService paymentService;
    private final String gatewayToken;

    public OrderController(IdempotentService idempotentService, IOrderService orderService,
                           PaymentService paymentService,
                           @Value("${nursing.gateway.trusted-token:}") String gatewayToken) {
        this.idempotentService = idempotentService;
        this.orderService = orderService;
        this.paymentService = paymentService;
        this.gatewayToken = gatewayToken;
    }

    @PostMapping("/prepay-token")
    public Result<PrepayTokenResponse> prepayToken(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Gateway-Token", required = false) String trustedToken) {
        requireTrustedGateway(trustedToken);
        return Result.success(idempotentService.issuePrepayToken(requireUserId(userId)));
    }

    @PostMapping
    public Result<OrderCreateResponse> create(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Gateway-Token", required = false) String trustedToken,
            @RequestHeader("Idempotency-Key") String idempotentKey,
            @Valid @RequestBody OrderCreateRequest request) {
        requireTrustedGateway(trustedToken);
        return Result.success(orderService.createOrder(requireUserId(userId), idempotentKey, request));
    }

    @GetMapping
    public Result<PageResult<OrderListResponse>> list(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Gateway-Token", required = false) String trustedToken,
            @Valid OrderPageQuery query) {
        requireTrustedGateway(trustedToken);
        return Result.success(orderService.listOrders(requireUserId(userId), query));
    }

    @GetMapping("/{id}")
    public Result<OrderDetailResponse> detail(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Gateway-Token", required = false) String trustedToken,
            @PathVariable("id") @Positive(message = "order id must be positive") Long id) {
        requireTrustedGateway(trustedToken);
        return Result.success(orderService.getOrderDetail(requireUserId(userId), id));
    }

    @PostMapping("/{id}/cancel")
    public Result<CancelResponse> cancel(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Gateway-Token", required = false) String trustedToken,
            @PathVariable("id") @Positive(message = "order id must be positive") Long id,
            @Valid @RequestBody(required = false) CancelOrderRequest request) {
        requireTrustedGateway(trustedToken);
        return Result.success(orderService.cancelOrder(requireUserId(userId), id, request));
    }

    @PostMapping({"/{id}/confirm", "/{id}/complete"})
    public Result<com.nursing.common.dto.OrderDTO> complete(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Gateway-Token", required = false) String trustedToken,
            @PathVariable("id") @Positive(message = "order id must be positive") Long id) {
        requireTrustedGateway(trustedToken);
        return Result.success(orderService.completeOrder(requireUserId(userId), id));
    }

    @PostMapping("/{id}/pay")
    public Result<PayResponse> pay(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Gateway-Token", required = false) String trustedToken,
            @PathVariable("id") @Positive(message = "order id must be positive") Long id,
            @RequestHeader("Idempotency-Key") String idempotentKey,
            @Valid @RequestBody PayRequest request) {
        requireTrustedGateway(trustedToken);
        return Result.success(paymentService.initiatePayment(requireUserId(userId), id, request, idempotentKey));
    }

    @PostMapping("/pay/callback")
    public String payCallback(@RequestParam Map<String, String> params) {
        return paymentService.handleAlipayCallback(params);
    }

    private Long requireUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(ApiCode.UNAUTHORIZED, "Unauthorized");
        }
        return userId;
    }

    private void requireTrustedGateway(String trustedToken) {
        if (!StringUtils.hasText(gatewayToken) || !gatewayToken.equals(trustedToken)) {
            throw new BusinessException(ApiCode.FORBIDDEN, "Forbidden", HttpStatus.FORBIDDEN);
        }
    }
}
