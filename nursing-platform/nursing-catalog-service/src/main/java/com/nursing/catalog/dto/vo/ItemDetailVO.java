package com.nursing.catalog.dto.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class ItemDetailVO {
    private Long itemId;
    private Long categoryId;
    private String categoryName;
    private String name;
    private String description;
    private String coverImage;
    private List<String> images = new ArrayList<>();
    private Integer sortOrder;
    private Integer status;
    private List<SpecVO> specs = new ArrayList<>();
    private LocalDateTime createTime;
}
