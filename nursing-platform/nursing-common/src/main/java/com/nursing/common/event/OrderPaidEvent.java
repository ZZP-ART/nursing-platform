package com.nursing.common.event;

import java.time.LocalDateTime;

public record OrderPaidEvent(
        String eventType,
        Long orderId,
        String orderNo,
        Long userId,
        Long serviceItemId,
        LocalDateTime paidAt) {
    public static final String TYPE = "ORDER_PAID";

    public static OrderPaidEvent of(Long orderId,
                                    String orderNo,
                                    Long userId,
                                    Long serviceItemId,
                                    LocalDateTime paidAt) {
        return new OrderPaidEvent(TYPE, orderId, orderNo, userId, serviceItemId, paidAt);
    }
}
