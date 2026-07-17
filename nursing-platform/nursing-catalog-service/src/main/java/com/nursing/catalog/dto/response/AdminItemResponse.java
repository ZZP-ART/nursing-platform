package com.nursing.catalog.dto.response;

import com.nursing.catalog.entity.ServiceItem;
import java.time.LocalDateTime;

public record AdminItemResponse(Long id, Long categoryId, String name, String description,
                                String coverImage, Integer sortOrder, Integer status,
                                LocalDateTime createTime, LocalDateTime updateTime) {
    public static AdminItemResponse from(ServiceItem item) {
        return new AdminItemResponse(item.getId(), item.getCategoryId(), item.getName(), item.getDescription(),
                item.getCoverImage(), item.getSortOrder(), item.getStatus(), item.getCreateTime(), item.getUpdateTime());
    }
}
