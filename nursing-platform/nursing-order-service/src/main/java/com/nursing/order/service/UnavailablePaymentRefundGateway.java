package com.nursing.order.service;

import com.nursing.order.entity.RefundRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(PaymentRefundGateway.class)
public class UnavailablePaymentRefundGateway implements PaymentRefundGateway {
    @Override
    public String refund(RefundRecord record) {
        throw new IllegalStateException("No production refund gateway is configured");
    }
}
