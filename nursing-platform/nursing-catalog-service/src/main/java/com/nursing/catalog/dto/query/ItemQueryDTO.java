package com.nursing.catalog.dto.query;

import lombok.Data;

@Data
public class ItemQueryDTO {
    private Long categoryId;
    private Integer page = 1;
    private Integer size = 20;
}
