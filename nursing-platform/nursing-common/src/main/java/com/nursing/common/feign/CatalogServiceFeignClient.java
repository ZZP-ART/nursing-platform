package com.nursing.common.feign;

import com.nursing.common.dto.ItemPriceDTO;
import com.nursing.common.dto.ServiceItemDTO;
import com.nursing.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "nursing-catalog-service", path = "/api/v1/items")
public interface CatalogServiceFeignClient {
    @GetMapping("/{id}")
    Result<ServiceItemDTO> getItemDetail(@PathVariable("id") Long id);

    @GetMapping("/{id}")
    Result<ItemPriceDTO> getItemPrice(@PathVariable("id") Long id);
}
