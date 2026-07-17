package com.nursing.catalog.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class MerchantServiceResponse {
    private Long itemId;
    private Long categoryId;
    private String categoryName;
    private String name;
    private String description;
    private String coverImage;
    private BigDecimal minPrice;
    private Integer version;
    private String auditStatus;
    private String publishStatus;
    private List<ServiceSpecResponse> specs;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
