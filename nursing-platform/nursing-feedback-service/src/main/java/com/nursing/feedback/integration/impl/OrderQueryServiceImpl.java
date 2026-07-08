package com.nursing.feedback.integration.impl;

import com.nursing.common.constant.ApiCode;
import com.nursing.common.dto.OrderDTO;
import com.nursing.common.feign.OrderFeignClient;
import com.nursing.common.result.Result;
import com.nursing.feedback.integration.OrderQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OrderQueryServiceImpl implements OrderQueryService {
    private static final Logger log = LoggerFactory.getLogger(OrderQueryServiceImpl.class);

    private final OrderFeignClient orderFeignClient;

    public OrderQueryServiceImpl(OrderFeignClient orderFeignClient) {
        this.orderFeignClient = orderFeignClient;
    }

    @Override
    public OrderDTO getOrder(Long orderId) {
        try {
            Result<OrderDTO> result = orderFeignClient.getOrder(orderId);
            if (result == null || result.getCode() != ApiCode.SUCCESS) {
                log.warn("Order query returned non-success response: orderId={}, code={}, message={}",
                        orderId, result == null ? null : result.getCode(), result == null ? null : result.getMessage());
                return null;
            }
            return result.getData();
        } catch (Exception ex) {
            log.warn("Order query fallback triggered: orderId={}", orderId, ex);
            return null;
        }
    }
}
