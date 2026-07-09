package com.nursing.common.feign;

import com.nursing.common.dto.OrderDTO;
import com.nursing.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "nursing-order-service", path = "/internal/v1/orders")
public interface OrderFeignClient {
    @GetMapping("/{id}")
    Result<OrderDTO> getOrder(@PathVariable("id") Long id,
                              @RequestHeader("X-Internal-Token") String internalToken);
}
