package com.nursing.catalog.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ServiceItem {
    private Long id;
    private Long categoryId;
    private String name;
    private String description;
    private String coverImage;
    private Integer status;
    private Integer sortOrder;
    private Integer isDeleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
