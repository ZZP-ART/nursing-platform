package com.nursing.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "nursing.jwt")
public class GatewayJwtProperties {
    private String secret;
    private String gatewayToken;
    private List<String> publicPaths = new ArrayList<>(List.of(
            "/api/v1/users/sms-code",
            "/api/v1/users/register",
            "/api/v1/users/login",
            "/api/v1/users/password/reset",
            "/api/v1/admin/login",
            "/api/v1/categories/**",
            "/api/v1/items/**",
            "/api/v1/orders/pay/callback"
    ));

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getGatewayToken() {
        return gatewayToken;
    }

    public void setGatewayToken(String gatewayToken) {
        this.gatewayToken = gatewayToken;
    }

    public List<String> getPublicPaths() {
        return publicPaths;
    }

    public void setPublicPaths(List<String> publicPaths) {
        this.publicPaths = publicPaths;
    }
}
