package com.nursing.order.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record OrderListResponse(
        Long orderId,
        String orderNo,
        String serviceItemName,
        String specName,
        BigDecimal specPrice,
        BigDecimal totalAmount,
        Integer status,
        LocalDate serviceDate,
        String serviceTimeSlot,
        String receiverName,
        String receiverPhone,
        String addressDetail,
        LocalDateTime createTime) {
}
