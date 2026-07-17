package com.nursing.catalog.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminCategoryRequest {
    private Long parentId;

    @NotBlank
    private String name;

    private String icon;
    private Integer sortOrder;
    private Integer status;
}
