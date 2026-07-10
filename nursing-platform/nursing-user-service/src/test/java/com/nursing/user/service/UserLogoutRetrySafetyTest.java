package com.nursing.user.service;

import com.nursing.common.util.SnowflakeIdWorker;
import com.nursing.user.config.JwtProperties;
import com.nursing.user.constant.UserErrorCode;
import com.nursing.user.exception.UserBusinessException;
import com.nursing.user.interceptor.UserTokenInterceptor;
import com.nursing.user.mapper.UserMapper;
import com.nursing.user.mapper.UserTokenMapper;
import com.nursing.user.security.IdCardCrypto;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserLogoutRetrySafetyTest {
    private static final String SECRET = "dev-jwt-secret-for-nursing-platform-32bytes";
    private static final String GATEWAY_TOKEN = "dev-gateway-token-for-nursing-platform";

    private RedisTemplate<String, String> redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private TokenService tokenService;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecret(SECRET);
        jwtProperties.setExpireSeconds(3600);
        redisTemplate = mock(RedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        tokenService = new TokenService(
                jwtProperties,
                redisTemplate,
                mock(UserTokenMapper.class),
                mock(SnowflakeIdWorker.class));
    }

    @Test
    void protectedPathRejectsBlacklistedToken() {
        String tokenId = UUID.randomUUID().toString();
        when(redisTemplate.hasKey("jwt:blacklist:" + tokenId)).thenReturn(true);
        UserTokenInterceptor interceptor = localTokenInterceptor();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users/profile");
        request.addHeader("Authorization", "Bearer " + token(tokenId, 10001L));

        assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()))
                .isInstanceOfSatisfying(UserBusinessException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(UserErrorCode.TOKEN_BLACKLISTED));
    }

    @Test
    void logoutPathAllowsBlacklistedTokenThroughInterceptor() {
        String tokenId = UUID.randomUUID().toString();
        UserTokenInterceptor interceptor = localTokenInterceptor();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/users/logout");
        request.addHeader("Authorization", "Bearer " + token(tokenId, 10001L));

        boolean result = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertThat(result).isTrue();
        assertThat(request.getAttribute("userId")).isEqualTo(10001L);
        assertThat(request.getAttribute("token")).isEqualTo(tokenService.resolveBearerToken(request.getHeader("Authorization")));
        verify(redisTemplate, never()).hasKey("jwt:blacklist:" + tokenId);
    }

    @Test
    void trustedGatewayHeaderInjectsUserIdWithoutLocalJwtValidation() {
        String tokenId = UUID.randomUUID().toString();
        UserTokenInterceptor interceptor = gatewayOnlyInterceptor();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users/profile");
        request.addHeader("X-Gateway-Token", GATEWAY_TOKEN);
        request.addHeader("X-User-Id", "10001");
        request.addHeader("Authorization", "Bearer " + token(tokenId, 10001L));

        boolean result = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertThat(result).isTrue();
        assertThat(request.getAttribute("userId")).isEqualTo(10001L);
        assertThat(request.getAttribute("token")).isEqualTo(tokenService.resolveBearerToken(request.getHeader("Authorization")));
        verify(redisTemplate, never()).hasKey("jwt:blacklist:" + tokenId);
    }

    @Test
    void logoutCanBeCalledTwiceWithSameToken() {
        String tokenId = UUID.randomUUID().toString();
        String token = token(tokenId, 10001L);
        UserService userService = new UserService(
                mock(UserMapper.class),
                mock(SmsService.class),
                tokenService,
                mock(PasswordEncoder.class),
                mock(SnowflakeIdWorker.class),
                mock(IdCardCrypto.class));

        assertThatCode(() -> {
            userService.logout("Bearer " + token);
            userService.logout("Bearer " + token);
        }).doesNotThrowAnyException();

        verify(valueOperations, times(2)).set(eq("jwt:blacklist:" + tokenId), eq("1"), anyLong(), eq(TimeUnit.SECONDS));
        verify(redisTemplate, never()).hasKey("jwt:blacklist:" + tokenId);
    }

    private UserTokenInterceptor localTokenInterceptor() {
        return new UserTokenInterceptor(tokenService, GATEWAY_TOKEN, true);
    }

    private UserTokenInterceptor gatewayOnlyInterceptor() {
        return new UserTokenInterceptor(tokenService, GATEWAY_TOKEN, false);
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
