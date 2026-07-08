package com.nursing.order.dto.response;

import java.math.BigDecimal;
import java.util.Map;

public record PayResponse(
        Long orderId,
        String orderNo,
        String payChannel,
        BigDecimal payAmount,
        String payStatus,
        boolean mock,
        Map<String, String> payParams) {
}
