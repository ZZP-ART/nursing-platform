package com.nursing.feedback.integration;

import com.nursing.common.dto.OrderDTO;

public interface OrderQueryService {
    OrderDTO getOrder(Long orderId);
    java.util.List<OrderDTO> getOrders(java.util.List<Long> orderIds);
}
