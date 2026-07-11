package com.nursing.catalog.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ServiceCategory {
    private Long id;
    private Long parentId;
    private String path;
    private Integer level;
    private String name;
    private String icon;
    private Integer sortOrder;
    private Integer status;
    private Integer isDeleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
