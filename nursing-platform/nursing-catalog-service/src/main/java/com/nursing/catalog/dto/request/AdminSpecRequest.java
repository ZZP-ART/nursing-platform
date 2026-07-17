package com.nursing.catalog.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class AdminSpecRequest {
    @NotNull
    private Long serviceItemId;

    @NotBlank
    private String name;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal price;

    private BigDecimal originalPrice;

    @Min(1)
    private Integer duration;

    private Integer status;
}
