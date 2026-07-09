package com.nursing.user.config;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class JwtSecurityValidator {
    private final JwtProperties jwtProperties;

    public JwtSecurityValidator(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    @PostConstruct
    void validate() {
        if (!StringUtils.hasText(jwtProperties.getSecret())) {
            throw new IllegalStateException("NURSING_JWT_SECRET is required");
        }
        if (jwtProperties.getSecret().getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("NURSING_JWT_SECRET must be at least 32 bytes for HS256");
        }
    }
}
