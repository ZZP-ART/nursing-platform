package com.nursing.order.controller;

import com.nursing.common.constant.ApiCode;
import com.nursing.common.dto.OrderDTO;
import com.nursing.common.exception.BusinessException;
import com.nursing.common.result.Result;
import com.nursing.order.service.IOrderService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;
import com.nursing.order.dto.request.InternalOrderTransitionRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/internal/v1/orders")
public class InternalOrderController {
    private final IOrderService orderService;
    private final String internalToken;

    public InternalOrderController(IOrderService orderService,
                                   @Value("${nursing.internal.token:}") String internalToken) {
        this.orderService = orderService;
        this.internalToken = internalToken;
    }

    @GetMapping("/{id}")
    public Result<OrderDTO> getOrder(@PathVariable("id") Long orderId,
                                     @RequestHeader(value = "X-Internal-Token", required = false) String token) {
        if (!StringUtils.hasText(internalToken) || !internalToken.equals(token)) {
            throw new BusinessException(ApiCode.FORBIDDEN, "内部服务鉴权失败", HttpStatus.FORBIDDEN);
        }
        return Result.success(orderService.getInternalOrder(orderId));
    }

    @PostMapping("/batch")
    public Result<List<OrderDTO>> getOrders(@RequestBody List<Long> orderIds,
                                            @RequestHeader(value = "X-Internal-Token", required = false) String token) {
        if (!StringUtils.hasText(internalToken) || !internalToken.equals(token)) {
            throw new BusinessException(ApiCode.FORBIDDEN, "内部服务鉴权失败", HttpStatus.FORBIDDEN);
        }
        return Result.success(orderService.getInternalOrders(orderIds));
    }

    @GetMapping("/merchant/{merchantId}")
    public Result<List<OrderDTO>> getMerchantOrders(@PathVariable Long merchantId,
                                                     @RequestParam(required = false) Integer status,
                                                     @RequestHeader(value = "X-Internal-Token", required = false) String token) {
        if (!StringUtils.hasText(internalToken) || !internalToken.equals(token)) {
            throw new BusinessException(ApiCode.FORBIDDEN, "内部服务鉴权失败", HttpStatus.FORBIDDEN);
        }
        return Result.success(orderService.getInternalMerchantOrders(merchantId, status));
    }

    @PostMapping("/{id}/transition")
    public Result<OrderDTO> transition(@PathVariable("id") Long orderId,
                                       @Valid @RequestBody InternalOrderTransitionRequest request,
                                       @RequestHeader(value = "X-Internal-Token", required = false) String token) {
        if (!StringUtils.hasText(internalToken) || !internalToken.equals(token)) {
            throw new BusinessException(ApiCode.FORBIDDEN, "内部服务鉴权失败", HttpStatus.FORBIDDEN);
        }
        return Result.success(orderService.transitionInternalOrder(orderId, request.getFromStatus(), request.getToStatus(), request.getReason()));
    }
}
