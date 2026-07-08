package com.nursing.catalog.dto.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SpecVO {
    private Long specId;
    private String name;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer duration;
    private Integer status;
}
