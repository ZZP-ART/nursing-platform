package com.nursing.order.dto.response;

public record CancelResponse(Long orderId, Integer status, String refundStatus) {
}
