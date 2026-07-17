package com.nursing.operations.feign;

import com.nursing.common.dto.OrderDTO;
import com.nursing.common.result.Result;
import java.util.Map;
import java.util.List;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "nursing-order-service")
public interface OrderServiceFeignClient {
    @GetMapping("/internal/v1/orders/{id}")
    Result<OrderDTO> getOrder(@PathVariable("id") Long orderId, @RequestHeader("X-Internal-Token") String token);

    @PostMapping("/internal/v1/orders/{id}/transition")
    Result<OrderDTO> transition(@PathVariable("id") Long orderId, @RequestBody Map<String, Object> request,
                                @RequestHeader("X-Internal-Token") String token);

    @GetMapping("/internal/v1/orders/merchant/{merchantId}")
    Result<List<OrderDTO>> getMerchantOrders(@PathVariable("merchantId") Long merchantId,
                                             @RequestParam(value = "status", required = false) Integer status,
                                             @RequestHeader("X-Internal-Token") String token);
}
