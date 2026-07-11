package com.nursing.common.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogOrderContractTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void orderDtoCanReadCatalogLegacyItemAndSpecIds() throws Exception {
        String json = """
                {
                  "itemId": 201,
                  "categoryId": 101,
                  "categoryName": "康复护理",
                  "name": "上门护理",
                  "status": 1,
                  "specs": [
                    {"specId": 301, "name": "单次服务", "price": 150.00, "status": 1}
                  ]
                }
                """;

        ServiceItemDTO item = objectMapper.readValue(json, ServiceItemDTO.class);

        assertThat(item.getId()).isEqualTo(201L);
        assertThat(item.getCategoryId()).isEqualTo(101L);
        assertThat(item.getCategoryName()).isEqualTo("康复护理");
        assertThat(item.getSpecs()).hasSize(1);
        assertThat(item.getSpecs().getFirst().getId()).isEqualTo(301L);
    }
}
