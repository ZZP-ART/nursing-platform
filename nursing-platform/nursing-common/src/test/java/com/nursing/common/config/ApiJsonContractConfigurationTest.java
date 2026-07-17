package com.nursing.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class ApiJsonContractConfigurationTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new ApiJsonContractConfiguration().apiIdSerializationModule());

    @Test
    void serializesLongIdPropertiesAsStringsWithoutChangingNumericQuantities() throws Exception {
        String json = objectMapper.writeValueAsString(new ApiPayload(2077640733917065216L, 42L, 300L));

        assertThat(json).contains("\"orderId\":\"2077640733917065216\"");
        assertThat(json).contains("\"id\":\"42\"");
        assertThat(json).contains("\"total\":300");
    }

    private record ApiPayload(Long orderId, Long id, long total) {
    }
}
