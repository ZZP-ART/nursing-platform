package com.nursing.order.service;

import com.nursing.order.dto.request.OrderCreateRequest;
import com.nursing.order.dto.response.OrderCreateResponse;

public interface IOrderService {
    OrderCreateResponse createOrder(Long userId, String idempotentKey, OrderCreateRequest request);
}
