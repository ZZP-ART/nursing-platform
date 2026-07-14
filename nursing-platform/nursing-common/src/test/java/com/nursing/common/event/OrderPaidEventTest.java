package com.nursing.common.event;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class OrderPaidEventTest {
    @Test
    void factoryCreatesTheSharedPaymentEventContract() {
        LocalDateTime paidAt = LocalDateTime.of(2026, 7, 13, 10, 0);

        OrderPaidEvent event = OrderPaidEvent.of(20001L, "NO-20001", 10001L, 201L, paidAt);

        assertEquals(OrderPaidEvent.TYPE, event.eventType());
        assertEquals(201L, event.serviceItemId());
        assertEquals(paidAt, event.paidAt());
    }
}
