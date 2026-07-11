package com.nursing.catalog.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class ItemDetailResponse {
    @JsonProperty("id")
    private Long itemId;
    private Long categoryId;
    private String categoryName;
    private String name;
    private String description;
    private String coverImage;
    private List<String> images = new ArrayList<>();
    private Integer sortOrder;
    private Integer status;
    private List<ServiceSpecResponse> specs = new ArrayList<>();
    private LocalDateTime createTime;
}
