package com.nursing.catalog.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Data
public class MerchantServiceRequest {
    @NotNull
    private Long categoryId;

    @NotBlank
    @Size(max = 30)
    private String name;

    @NotBlank
    @Size(min = 10, max = 2000)
    private String description;

    @Size(max = 256)
    private String coverImage;

    @Valid
    @NotEmpty
    @Size(max = 6)
    private List<SpecRequest> specs;

    @Data
    public static class SpecRequest {
        @NotBlank
        @Size(max = 20)
        private String name;

        @NotNull
        @DecimalMin(value = "0.01")
        private BigDecimal price;

        @DecimalMin(value = "0.01")
        private BigDecimal originalPrice;

        @NotNull
        @DecimalMin(value = "1")
        private Integer duration;
    }
}
