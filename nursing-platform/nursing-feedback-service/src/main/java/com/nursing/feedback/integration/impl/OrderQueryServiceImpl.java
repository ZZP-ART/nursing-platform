package com.nursing.feedback.integration.impl;

import com.nursing.common.constant.ApiCode;
import com.nursing.common.dto.OrderDTO;
import com.nursing.feedback.feign.OrderFeignClient;
import com.nursing.common.result.Result;
import com.nursing.feedback.integration.OrderQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import com.nursing.common.exception.BusinessException;

@Service
public class OrderQueryServiceImpl implements OrderQueryService {
    private static final Logger log = LoggerFactory.getLogger(OrderQueryServiceImpl.class);

    private final OrderFeignClient orderFeignClient;
    private final String internalToken;

    public OrderQueryServiceImpl(OrderFeignClient orderFeignClient,
                                 @Value("${nursing.internal.token:}") String internalToken) {
        this.orderFeignClient = orderFeignClient;
        this.internalToken = internalToken;
    }

    @Override
    public OrderDTO getOrder(Long orderId) {
        try {
            Result<OrderDTO> result = orderFeignClient.getOrder(orderId, internalToken);
            if (result != null && result.getCode() == ApiCode.NOT_FOUND) {
                return null;
            }
            if (result == null || result.getCode() != ApiCode.SUCCESS) {
                log.warn("Order query returned non-success response: orderId={}, code={}, message={}",
                        orderId, result == null ? null : result.getCode(), result == null ? null : result.getMessage());
                throw new BusinessException(ApiCode.DEPENDENCY_UNAVAILABLE, "Order service is unavailable", HttpStatus.SERVICE_UNAVAILABLE);
            }
            return result.getData();
        } catch (Exception ex) {
            log.warn("Order query failed: orderId={}", orderId, ex);
            throw new BusinessException(ApiCode.DEPENDENCY_UNAVAILABLE, "Order service is unavailable", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    @Override
    public java.util.List<OrderDTO> getOrders(java.util.List<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) return java.util.List.of();
        try {
            Result<java.util.List<OrderDTO>> result = orderFeignClient.getOrders(orderIds, internalToken);
            if (result == null || result.getCode() != ApiCode.SUCCESS || result.getData() == null) {
                throw new BusinessException(ApiCode.DEPENDENCY_UNAVAILABLE, "Order service is unavailable", HttpStatus.SERVICE_UNAVAILABLE);
            }
            return result.getData();
        } catch (BusinessException ex) { throw ex; }
        catch (Exception ex) { throw new BusinessException(ApiCode.DEPENDENCY_UNAVAILABLE, "Order service is unavailable", HttpStatus.SERVICE_UNAVAILABLE); }
    }
}
