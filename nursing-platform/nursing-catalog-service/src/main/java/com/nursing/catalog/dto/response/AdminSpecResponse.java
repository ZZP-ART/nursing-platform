package com.nursing.catalog.dto.response;

import com.nursing.catalog.entity.ServiceSpec;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminSpecResponse(Long id, Long serviceItemId, String name, BigDecimal price,
                                BigDecimal originalPrice, Integer duration, Integer status,
                                LocalDateTime createTime, LocalDateTime updateTime) {
    public static AdminSpecResponse from(ServiceSpec spec) {
        return new AdminSpecResponse(spec.getId(), spec.getServiceItemId(), spec.getName(), spec.getPrice(),
                spec.getOriginalPrice(), spec.getDuration(), spec.getStatus(), spec.getCreateTime(), spec.getUpdateTime());
    }
}
