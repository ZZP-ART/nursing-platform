package com.nursing.catalog.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ServiceItem {
    private Long id;
    private Long categoryId;
    private Long ownerUserId;
    private String name;
    private String description;
    private String coverImage;
    private Integer status;
    private String auditStatus;
    private String publishStatus;
    private Integer version;
    private Integer sortOrder;
    private Integer isDeleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
