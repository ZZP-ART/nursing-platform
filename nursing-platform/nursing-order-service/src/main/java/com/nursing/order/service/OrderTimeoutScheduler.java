package com.nursing.order.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OrderTimeoutScheduler {
    private final IOrderService orderService;
    private final int timeoutMinutes;

    public OrderTimeoutScheduler(IOrderService orderService,
                                 @Value("${nursing.order.payment-timeout-minutes:30}") int timeoutMinutes) {
        this.orderService = orderService;
        this.timeoutMinutes = timeoutMinutes;
    }

    @Scheduled(fixedDelayString = "${nursing.order.timeout-scan-fixed-delay:60000}")
    public void cancelExpiredPendingPaymentOrders() {
        orderService.cancelExpiredPendingPaymentOrders(timeoutMinutes);
    }
}
