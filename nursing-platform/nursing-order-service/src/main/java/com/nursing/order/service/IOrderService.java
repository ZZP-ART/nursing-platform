package com.nursing.order.service;

import com.nursing.common.result.PageResult;
import com.nursing.common.dto.OrderDTO;
import com.nursing.order.dto.request.CancelOrderRequest;
import com.nursing.order.dto.request.OrderCreateRequest;
import com.nursing.order.dto.request.OrderPageQuery;
import com.nursing.order.dto.response.CancelResponse;
import com.nursing.order.dto.response.OrderCreateResponse;
import com.nursing.order.dto.response.OrderDetailResponse;
import com.nursing.order.dto.response.OrderListResponse;

public interface IOrderService {
    OrderCreateResponse createOrder(Long userId, String idempotentKey, OrderCreateRequest request);

    PageResult<OrderListResponse> listOrders(Long userId, OrderPageQuery query);

    OrderDetailResponse getOrderDetail(Long userId, Long orderId);

    OrderDTO getInternalOrder(Long orderId);

    java.util.List<OrderDTO> getInternalOrders(java.util.List<Long> orderIds);

    CancelResponse cancelOrder(Long userId, Long orderId, CancelOrderRequest request);

    OrderDTO completeOrder(Long userId, Long orderId);

    int cancelExpiredPendingPaymentOrders(int timeoutMinutes);
}
