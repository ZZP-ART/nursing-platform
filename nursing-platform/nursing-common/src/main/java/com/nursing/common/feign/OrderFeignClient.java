package com.nursing.common.feign;

import com.nursing.common.dto.OrderDTO;
import com.nursing.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "nursing-order-service", path = "/api/v1/orders")
public interface OrderFeignClient {
    @GetMapping("/{id}")
    Result<OrderDTO> getOrder(@PathVariable("id") Long id);
}
