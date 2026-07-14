package com.nursing.common.feign;

import com.nursing.common.dto.OrderDTO;
import com.nursing.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.List;

@FeignClient(name = "nursing-order-service", path = "/internal/v1/orders")
public interface OrderFeignClient {
    @GetMapping("/{id}")
    Result<OrderDTO> getOrder(@PathVariable("id") Long id,
                              @RequestHeader("X-Internal-Token") String internalToken);

    @PostMapping("/batch")
    Result<List<OrderDTO>> getOrders(@RequestBody List<Long> orderIds,
                                     @RequestHeader("X-Internal-Token") String internalToken);
}
