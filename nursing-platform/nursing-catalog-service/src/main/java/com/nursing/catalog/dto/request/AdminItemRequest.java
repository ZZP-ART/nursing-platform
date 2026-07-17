package com.nursing.catalog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminItemRequest {
    @NotNull
    private Long categoryId;

    @NotBlank
    private String name;

    private String description;
    private String coverImage;
    private Integer sortOrder;
    private Integer status;
}
