package com.nursing.catalog.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MerchantServiceListResponse {
    private List<MerchantServiceResponse> list;
    private Summary summary;

    @Data
    @AllArgsConstructor
    public static class Summary {
        private int total;
        private int draft;
        private int pending;
        private int approved;
        private int rejected;
        private int published;
    }
}
