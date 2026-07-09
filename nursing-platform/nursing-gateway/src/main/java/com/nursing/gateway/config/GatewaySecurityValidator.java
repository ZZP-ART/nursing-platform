package com.nursing.gateway.config;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class GatewaySecurityValidator {
    private final GatewayJwtProperties properties;

    public GatewaySecurityValidator(GatewayJwtProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void validate() {
        if (!StringUtils.hasText(properties.getSecret())) {
            throw new IllegalStateException("NURSING_JWT_SECRET is required");
        }
        if (properties.getSecret().getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("NURSING_JWT_SECRET must be at least 32 bytes for HS256");
        }
        if (!StringUtils.hasText(properties.getGatewayToken())) {
            throw new IllegalStateException("NURSING_GATEWAY_TRUSTED_TOKEN is required");
        }
    }
}
