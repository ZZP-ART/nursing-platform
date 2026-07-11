package com.nursing.catalog.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ServiceSpecResponse {
    @JsonProperty("id")
    private Long specId;
    private Long serviceItemId;
    private String name;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer duration;
    private Integer status;
}
