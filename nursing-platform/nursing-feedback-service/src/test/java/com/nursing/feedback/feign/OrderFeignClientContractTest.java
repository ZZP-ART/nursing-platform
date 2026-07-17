package com.nursing.feedback.feign;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

class OrderFeignClientContractTest {
    @Test
    void orderFeignUsesInternalPathAndRequiresInternalTokenHeader() throws Exception {
        Method method = OrderFeignClient.class.getMethod("getOrder", Long.class, String.class);

        assertThat(OrderFeignClient.class.getAnnotation(org.springframework.cloud.openfeign.FeignClient.class).path())
                .isEqualTo("/internal/v1/orders");
        assertThat(method.getAnnotation(GetMapping.class).value()).containsExactly("/{id}");
        RequestHeader header = method.getParameters()[1].getAnnotation(RequestHeader.class);
        assertThat(header.value()).isEqualTo("X-Internal-Token");
    }
}
