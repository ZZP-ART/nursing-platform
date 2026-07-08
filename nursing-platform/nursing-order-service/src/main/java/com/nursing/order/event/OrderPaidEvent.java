package com.nursing.order.event;

import java.time.LocalDateTime;

public record OrderPaidEvent(
        String eventType,
        Long orderId,
        String orderNo,
        Long userId,
        LocalDateTime paidAt) {
    public static OrderPaidEvent of(Long orderId, String orderNo, Long userId, LocalDateTime paidAt) {
        return new OrderPaidEvent("ORDER_PAID", orderId, orderNo, userId, paidAt);
    }
}
