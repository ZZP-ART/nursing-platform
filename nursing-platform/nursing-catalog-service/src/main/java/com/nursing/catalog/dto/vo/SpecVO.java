package com.nursing.catalog.dto.vo;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SpecVO {
    @JsonProperty("id")
    @JsonAlias("specId")
    private Long specId;
    private Long serviceItemId;
    private String name;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer duration;
    private Integer status;
}
