package com.nursing.catalog.dto.vo;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class ItemPageVO {
    @JsonProperty("id")
    @JsonAlias("itemId")
    private Long itemId;
    private Long categoryId;
    private String categoryName;
    private String name;
    private String description;
    private String coverImage;
    private Integer sortOrder;
    private Integer status;
    private BigDecimal minPrice;
    private List<SpecVO> specs = new ArrayList<>();
}
