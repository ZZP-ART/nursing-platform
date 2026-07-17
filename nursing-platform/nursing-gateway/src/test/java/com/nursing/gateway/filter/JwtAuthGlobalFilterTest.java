package com.nursing.gateway.filter;

import com.nursing.gateway.config.GatewayJwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyString;

class JwtAuthGlobalFilterTest {
    private static final String SECRET = "dev-jwt-secret-for-nursing-platform-32bytes";
    private ReactiveStringRedisTemplate redisTemplate;
    private ReactiveValueOperations<String, String> valueOperations;
    private JwtAuthGlobalFilter filter;

    @BeforeEach
    void setUp() {
        GatewayJwtProperties properties = new GatewayJwtProperties();
        properties.setSecret(SECRET);
        properties.setGatewayToken("gateway-token");
        redisTemplate = mock(ReactiveStringRedisTemplate.class);
        valueOperations = mock(ReactiveValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(Mono.just("1"));
        filter = new JwtAuthGlobalFilter(properties, redisTemplate);
    }

    @Test
    void publicPathPassesThroughAndStripsSpoofedUserHeaders() {
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/items/201")
                .header("X-User-Id", "999")
                .header("X-UserId", "999")
                .header("userId", "999"));

        filter.filter(exchange, next -> {
            forwarded.set(next);
            return Mono.empty();
        }).block();

        assertThat(forwarded.get().getRequest().getHeaders().containsKey("X-User-Id")).isFalse();
        assertThat(forwarded.get().getRequest().getHeaders().containsKey("X-UserId")).isFalse();
        assertThat(forwarded.get().getRequest().getHeaders().containsKey("userId")).isFalse();
    }

    @Test
    void protectedPathWithoutTokenReturns401() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/orders"));

        filter.filter(exchange, next -> Mono.empty()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void validJwtInjectsTrustedUserHeader() {
        String tokenId = UUID.randomUUID().toString();
        when(redisTemplate.hasKey("jwt:blacklist:" + tokenId)).thenReturn(Mono.just(false));
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(tokenId, 10001L))
                .header("X-User-Id", "999"));

        filter.filter(exchange, next -> {
            forwarded.set(next);
            return Mono.empty();
        }).block();

        assertThat(forwarded.get().getRequest().getHeaders().getFirst("X-User-Id")).isEqualTo("10001");
        assertThat(forwarded.get().getRequest().getHeaders().getFirst("X-Gateway-Token")).isEqualTo("gateway-token");
    }

    @Test
    void blacklistedJwtReturns401() {
        String tokenId = UUID.randomUUID().toString();
        when(redisTemplate.hasKey("jwt:blacklist:" + tokenId)).thenReturn(Mono.just(true));
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(tokenId, 10001L)));

        filter.filter(exchange, next -> Mono.empty()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void blacklistedJwtCanReachLogout() {
        String tokenId = UUID.randomUUID().toString();
        when(redisTemplate.hasKey("jwt:blacklist:" + tokenId)).thenReturn(Mono.just(true));
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/api/v1/users/logout")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(tokenId, 10001L))
                .header("X-User-Id", "999"));

        filter.filter(exchange, next -> {
            forwarded.set(next);
            return Mono.empty();
        }).block();

        assertThat(exchange.getResponse().getStatusCode()).isNull();
        assertThat(forwarded.get().getRequest().getHeaders().getFirst("X-User-Id")).isEqualTo("10001");
        assertThat(forwarded.get().getRequest().getHeaders().getFirst("X-Gateway-Token")).isEqualTo("gateway-token");
    }

    @Test
    void logoutWithoutTokenReturns401() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/api/v1/users/logout"));

        filter.filter(exchange, next -> Mono.empty()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void logoutWithInvalidTokenReturns401() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/api/v1/users/logout")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"));

        filter.filter(exchange, next -> Mono.empty()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private String token(String tokenId, Long userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(tokenId)
                .subject(String.valueOf(userId))
                .claim("userId", userId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(3600)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }
}
