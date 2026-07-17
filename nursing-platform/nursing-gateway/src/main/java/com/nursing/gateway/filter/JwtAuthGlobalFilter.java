package com.nursing.gateway.filter;

import com.nursing.gateway.config.GatewayJwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthGlobalFilter implements GlobalFilter, Ordered {
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";
    private static final String LOGOUT_PATH = "/api/v1/users/logout";
    private static final String AUTH_VERSION_PREFIX = "authz:version:";
    private static final List<String> UNTRUSTED_USER_HEADERS = List.of(
            "X-User-Id", "X-UserId", "userId", "X-User-Roles", "X-Gateway-Token");

    private final GatewayJwtProperties jwtProperties;
    private final ReactiveStringRedisTemplate redisTemplate;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public JwtAuthGlobalFilter(GatewayJwtProperties jwtProperties, ReactiveStringRedisTemplate redisTemplate) {
        this.jwtProperties = jwtProperties;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest sanitizedRequest = removeUserHeaders(exchange.getRequest());
        ServerWebExchange sanitizedExchange = exchange.mutate().request(sanitizedRequest).build();
        String path = sanitizedRequest.getPath().pathWithinApplication().value();
        if (isPublicPath(path)) {
            return chain.filter(sanitizedExchange);
        }

        String token = resolveBearerToken(sanitizedRequest.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
        if (!StringUtils.hasText(token)) {
            return unauthorized(sanitizedExchange, 1002, "Unauthorized");
        }

        Claims claims;
        try {
            claims = Jwts.parser().verifyWith(signingKey()).build().parseSignedClaims(token).getPayload();
        } catch (JwtException | IllegalArgumentException ex) {
            return unauthorized(sanitizedExchange, 1002, "Unauthorized");
        }

        String tokenId = claims.getId();
        Long userId = resolveUserId(claims);
        Integer authorizationVersion = claims.get("authorizationVersion", Integer.class);
        List<String> roles = resolveRoles(claims);
        if (!StringUtils.hasText(tokenId) || userId == null || userId <= 0) {
            return unauthorized(sanitizedExchange, 1002, "Unauthorized");
        }

        return redisTemplate.hasKey(BLACKLIST_PREFIX + tokenId)
                .onErrorReturn(true)
                .flatMap(blacklisted -> redisTemplate.opsForValue()
                        .get(AUTH_VERSION_PREFIX + userId)
                        .defaultIfEmpty("")
                        .flatMap(currentVersion -> authorizeRequest(
                                sanitizedExchange,
                                sanitizedRequest,
                                chain,
                                path,
                                userId,
                                authorizationVersion,
                                roles,
                                blacklisted,
                                currentVersion)));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private Mono<Void> authorizeRequest(ServerWebExchange sanitizedExchange,
                                        ServerHttpRequest sanitizedRequest,
                                        GatewayFilterChain chain,
                                        String path,
                                        Long userId,
                                        Integer authorizationVersion,
                                        List<String> roles,
                                        Boolean blacklisted,
                                        String currentVersion) {
        if (Boolean.TRUE.equals(blacklisted) && !isLogoutPath(path)) {
            return unauthorized(sanitizedExchange, 1003, "Token is blacklisted");
        }
        if (!StringUtils.hasText(currentVersion)) {
            return unauthorized(sanitizedExchange, 1003, "Authorization status is unavailable");
        }
        String tokenVersion = String.valueOf(authorizationVersion == null ? 1 : authorizationVersion);
        if (!tokenVersion.equals(currentVersion)) {
            return unauthorized(sanitizedExchange, 1003, "Token is expired");
        }
        if (!hasRouteRole(path, roles)) {
            return forbidden(sanitizedExchange);
        }

        ServerHttpRequest trustedRequest = sanitizedRequest.mutate()
                .header("X-User-Id", String.valueOf(userId))
                .header("X-User-Roles", String.join(",", roles))
                .header("X-Gateway-Token", jwtProperties.getGatewayToken())
                .build();
        return chain.filter(sanitizedExchange.mutate().request(trustedRequest).build());
    }

    private ServerHttpRequest removeUserHeaders(ServerHttpRequest request) {
        return request.mutate().headers(headers -> UNTRUSTED_USER_HEADERS.forEach(headers::remove)).build();
    }

    private boolean isPublicPath(String path) {
        return jwtProperties.getPublicPaths().stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private boolean isLogoutPath(String path) {
        return pathMatcher.match(LOGOUT_PATH, path);
    }

    private String resolveBearerToken(String authorizationHeader) {
        if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return authorizationHeader.substring(BEARER_PREFIX.length()).trim();
    }

    private SecretKey signingKey() {
        String secret = jwtProperties.getSecret();
        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException("NURSING_JWT_SECRET is required");
        }
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    private Long resolveUserId(Claims claims) {
        Number userId = claims.get("userId", Number.class);
        if (userId != null) {
            return userId.longValue();
        }
        String subject = claims.getSubject();
        if (!StringUtils.hasText(subject)) {
            return null;
        }
        try {
            return Long.valueOf(subject);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private List<String> resolveRoles(Claims claims) {
        Object value = claims.get("roles");
        if (!(value instanceof List<?> raw)) {
            return Collections.emptyList();
        }
        return raw.stream().filter(String.class::isInstance).map(String.class::cast).toList();
    }

    private boolean hasRouteRole(String path, List<String> roles) {
        if (path.startsWith("/api/v1/admin/") || path.startsWith("/api/v1/operations/")) {
            return roles.contains("ADMIN");
        }
        if (pathMatcher.match("/api/v1/caregiver/applications", path)
                || pathMatcher.match("/api/v1/caregiver/applications/**", path)) {
            return true;
        }
        if (path.startsWith("/api/v1/caregiver/")) {
            return roles.contains("CAREGIVER");
        }
        return true;
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, int code, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] bytes = ("{\"code\":" + code + ",\"message\":\"" + message + "\",\"data\":null}")
                .getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private Mono<Void> forbidden(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] bytes = "{\"code\":1004,\"message\":\"Forbidden\",\"data\":null}".getBytes(StandardCharsets.UTF_8);
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
    }
}
