package com.nursing.catalog.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ServiceSpec {
    private Long id;
    private Long serviceItemId;
    private String name;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer duration;
    private Integer status;
    private Integer isDeleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
