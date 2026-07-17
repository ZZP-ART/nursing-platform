package com.nursing.user.service;

import com.nursing.common.constant.ApiCode;
import com.nursing.common.util.SnowflakeIdWorker;
import com.nursing.user.config.JwtProperties;
import com.nursing.user.constant.UserErrorCode;
import com.nursing.user.entity.User;
import com.nursing.user.entity.UserToken;
import com.nursing.user.exception.UserBusinessException;
import com.nursing.user.mapper.UserTokenMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class TokenService {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String USER_TOKEN_KEY_PREFIX = "user:token:";
    private static final String TOKEN_BLACKLIST_KEY_PREFIX = "jwt:blacklist:";
    private static final String AUTH_VERSION_KEY_PREFIX = "authz:version:";

    private final JwtProperties jwtProperties;
    private final RedisTemplate<String, String> redisTemplate;
    private final UserTokenMapper userTokenMapper;
    private final SnowflakeIdWorker snowflakeIdWorker;
    private final ZoneId zoneId = ZoneId.systemDefault();

    public TokenService(JwtProperties jwtProperties,
                        RedisTemplate<String, String> redisTemplate,
                        UserTokenMapper userTokenMapper,
                        SnowflakeIdWorker snowflakeIdWorker) {
        this.jwtProperties = jwtProperties;
        this.redisTemplate = redisTemplate;
        this.userTokenMapper = userTokenMapper;
        this.snowflakeIdWorker = snowflakeIdWorker;
    }

    public TokenIssue generateToken(User user, List<String> roles) {
        Instant now = Instant.now();
        Instant expireAt = now.plusSeconds(jwtProperties.getExpireSeconds());
        String tokenId = UUID.randomUUID().toString();

        String token = Jwts.builder()
                .id(tokenId)
                .subject(String.valueOf(user.getId()))
                .claim("userId", user.getId())
                .claim("phone", user.getPhone())
                .claim("roles", roles == null ? List.of() : roles)
                .claim("authorizationVersion", user.getAuthorizationVersion() == null ? 1 : user.getAuthorizationVersion())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expireAt))
                .signWith(signingKey())
                .compact();

        LocalDateTime expireTime = LocalDateTime.ofInstant(expireAt, zoneId);
        saveTokenRecord(user.getId(), tokenId, token, expireTime);
        redisTemplate.opsForValue().set(AUTH_VERSION_KEY_PREFIX + user.getId(),
                String.valueOf(user.getAuthorizationVersion() == null ? 1 : user.getAuthorizationVersion()),
                Math.max(1L, jwtProperties.getExpireSeconds()), TimeUnit.SECONDS);
        return new TokenIssue(token, tokenId, expireTime);
    }

    public TokenPayload validateToken(String token) {
        return validateToken(token, true);
    }

    public TokenIssue generateToken(User user) {
        return generateToken(user, List.of());
    }

    public TokenPayload validateTokenForLogout(String token) {
        return validateToken(token, false);
    }

    private TokenPayload validateToken(String token, boolean rejectBlacklisted) {
        Claims claims = parseClaims(token);
        String tokenId = claims.getId();
        if (!StringUtils.hasText(tokenId)) {
            throw unauthorized();
        }
        if (rejectBlacklisted && Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey(tokenId)))) {
            throw new UserBusinessException(
                    HttpStatus.UNAUTHORIZED,
                    UserErrorCode.TOKEN_BLACKLISTED,
                    "Token 已被列入黑名单");
        }
        Long userId = claims.get("userId", Long.class);
        if (userId == null) {
            String subject = claims.getSubject();
            if (!StringUtils.hasText(subject)) {
                throw unauthorized();
            }
            userId = Long.valueOf(subject);
        }
        return new TokenPayload(userId, claims.get("phone", String.class), tokenId, claims.getExpiration());
    }

    public void invalidateToken(String token) {
        Claims claims = parseClaims(token);
        String tokenId = claims.getId();
        if (!StringUtils.hasText(tokenId)) {
            throw unauthorized();
        }
        long ttlSeconds = remainingSeconds(claims.getExpiration());
        if (ttlSeconds > 0) {
            // 登出不修改 JWT 本身，而是把 tokenId 拉黑到原过期时间。
            redisTemplate.opsForValue().set(blacklistKey(tokenId), "1", ttlSeconds, TimeUnit.SECONDS);
        }
    }

    public void cacheAuthorizationVersion(Long userId, Integer authorizationVersion) {
        redisTemplate.opsForValue().set(AUTH_VERSION_KEY_PREFIX + userId,
                String.valueOf(authorizationVersion == null ? 1 : authorizationVersion),
                Math.max(1L, jwtProperties.getExpireSeconds()), TimeUnit.SECONDS);
    }

    public String resolveBearerToken(String authorizationHeader) {
        if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw unauthorized();
        }
        String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
        if (!StringUtils.hasText(token)) {
            throw unauthorized();
        }
        return token;
    }

    private void saveTokenRecord(Long userId, String tokenId, String token, LocalDateTime expireTime) {
        UserToken userToken = new UserToken();
        LocalDateTime now = LocalDateTime.now();
        userToken.setId(snowflakeIdWorker.nextId());
        userToken.setUserId(userId);
        userToken.setToken(token);
        userToken.setExpireTime(expireTime);
        userToken.setIsDeleted(0);
        userToken.setCreateTime(now);
        userTokenMapper.insert(userToken);

        // Redis 记录的 TTL 与 JWT 有效期保持一致，避免过期登录态长期占用缓存。
        long ttlSeconds = Math.max(1L, jwtProperties.getExpireSeconds());
        redisTemplate.opsForValue().set(
                userTokenKey(userId, tokenId),
                expireTime.toString(),
                ttlSeconds,
                TimeUnit.SECONDS);
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException exception) {
            throw unauthorized();
        } catch (JwtException | IllegalArgumentException exception) {
            throw unauthorized();
        }
    }

    private SecretKey signingKey() {
        String secret = jwtProperties.getSecret();
        if (!StringUtils.hasText(secret)) {
            throw new UserBusinessException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    ApiCode.SERVER_ERROR,
                    "JWT密钥未配置");
        }
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    private long remainingSeconds(Date expiration) {
        return Math.max(0L, (expiration.getTime() - System.currentTimeMillis()) / 1000L);
    }

    private String userTokenKey(Long userId, String tokenId) {
        return USER_TOKEN_KEY_PREFIX + userId + ":" + tokenId;
    }

    private String blacklistKey(String tokenId) {
        return TOKEN_BLACKLIST_KEY_PREFIX + tokenId;
    }

    private UserBusinessException unauthorized() {
        return new UserBusinessException(HttpStatus.UNAUTHORIZED, ApiCode.UNAUTHORIZED, "未授权，请先登录");
    }

    public static class TokenIssue {
        private final String token;
        private final String tokenId;
        private final LocalDateTime expireTime;

        public TokenIssue(String token, String tokenId, LocalDateTime expireTime) {
            this.token = token;
            this.tokenId = tokenId;
            this.expireTime = expireTime;
        }

        public String getToken() {
            return token;
        }

        public String getTokenId() {
            return tokenId;
        }

        public LocalDateTime getExpireTime() {
            return expireTime;
        }
    }

    public static class TokenPayload {
        private final Long userId;
        private final String phone;
        private final String tokenId;
        private final Date expiration;

        public TokenPayload(Long userId, String phone, String tokenId, Date expiration) {
            this.userId = userId;
            this.phone = phone;
            this.tokenId = tokenId;
            this.expiration = expiration;
        }

        public Long getUserId() {
            return userId;
        }

        public String getPhone() {
            return phone;
        }

        public String getTokenId() {
            return tokenId;
        }

        public Date getExpiration() {
            return expiration;
        }
    }
}
