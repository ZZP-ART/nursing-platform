package com.nursing.catalog.dto.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CategoryTreeVO {
    private Long categoryId;
    private Long parentId;
    private String name;
    private String icon;
    private Integer sortOrder;
    private Integer status;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<CategoryTreeVO> children = new ArrayList<>();
}
