package com.nursing.order.service;
import com.nursing.order.entity.RefundRecord;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
@Component
@ConditionalOnProperty(name = "nursing.payment.mock", havingValue = "true")
public class MockPaymentRefundGateway implements PaymentRefundGateway {
    @Override public String refund(RefundRecord record) { return "mock-refund-" + record.getRefundNo(); }
}
