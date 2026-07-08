package com.nursing.order.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record OrderDetailResponse(
        Long orderId,
        String orderNo,
        String serviceItemName,
        String specName,
        BigDecimal specPrice,
        BigDecimal totalAmount,
        Integer status,
        String receiverName,
        String receiverPhone,
        String addressDetail,
        LocalDate serviceDate,
        String serviceTimeSlot,
        List<OrderOperationLogResponse> operationLogs,
        LocalDateTime createTime) {
}
