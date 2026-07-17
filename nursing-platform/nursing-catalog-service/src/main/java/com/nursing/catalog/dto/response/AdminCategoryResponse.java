package com.nursing.catalog.dto.response;

import com.nursing.catalog.entity.ServiceCategory;
import java.time.LocalDateTime;

public record AdminCategoryResponse(Long id, Long parentId, String name, String icon,
                                    Integer sortOrder, Integer status, LocalDateTime createTime,
                                    LocalDateTime updateTime) {
    public static AdminCategoryResponse from(ServiceCategory category) {
        return new AdminCategoryResponse(category.getId(), category.getParentId(), category.getName(), category.getIcon(),
                category.getSortOrder(), category.getStatus(), category.getCreateTime(), category.getUpdateTime());
    }
}
