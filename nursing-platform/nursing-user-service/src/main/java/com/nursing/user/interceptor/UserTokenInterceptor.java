package com.nursing.user.interceptor;

import com.nursing.common.constant.ApiCode;
import com.nursing.user.exception.UserBusinessException;
import com.nursing.user.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class UserTokenInterceptor implements HandlerInterceptor {
    private static final String LOGOUT_PATH = "/api/v1/users/logout";
    private static final String GATEWAY_TOKEN_HEADER = "X-Gateway-Token";
    private static final String USER_ID_HEADER = "X-User-Id";

    private final TokenService tokenService;
    private final String gatewayTrustedToken;
    private final boolean allowLocalTokenValidation;

    public UserTokenInterceptor(TokenService tokenService,
                                @Value("${nursing.gateway.trusted-token:}") String gatewayTrustedToken,
                                @Value("${nursing.auth.allow-local-token-validation:false}") boolean allowLocalTokenValidation) {
        this.tokenService = tokenService;
        this.gatewayTrustedToken = gatewayTrustedToken;
        this.allowLocalTokenValidation = allowLocalTokenValidation;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (isTrustedGatewayRequest(request)) {
            Long userId = resolveGatewayUserId(request);
            request.setAttribute("userId", userId);
            setTokenAttributeIfPresent(request);
            return true;
        }

        if (!allowLocalTokenValidation) {
            throw forbidden();
        }

        String token = tokenService.resolveBearerToken(request.getHeader("Authorization"));
        TokenService.TokenPayload payload = isLogoutRequest(request)
                ? tokenService.validateTokenForLogout(token)
                : tokenService.validateToken(token);
        // Controllers only read server-injected identity, never client-supplied userId.
        request.setAttribute("userId", payload.getUserId());
        request.setAttribute("token", token);
        return true;
    }

    private boolean isTrustedGatewayRequest(HttpServletRequest request) {
        String requestGatewayToken = request.getHeader(GATEWAY_TOKEN_HEADER);
        return StringUtils.hasText(gatewayTrustedToken) && gatewayTrustedToken.equals(requestGatewayToken);
    }

    private Long resolveGatewayUserId(HttpServletRequest request) {
        String userIdHeader = request.getHeader(USER_ID_HEADER);
        if (!StringUtils.hasText(userIdHeader)) {
            throw unauthorized();
        }
        try {
            Long userId = Long.valueOf(userIdHeader.trim());
            if (userId <= 0) {
                throw unauthorized();
            }
            return userId;
        } catch (NumberFormatException exception) {
            throw unauthorized();
        }
    }

    private void setTokenAttributeIfPresent(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");
        if (StringUtils.hasText(authorizationHeader)) {
            request.setAttribute("token", tokenService.resolveBearerToken(authorizationHeader));
        }
    }

    private boolean isLogoutRequest(HttpServletRequest request) {
        return LOGOUT_PATH.equals(request.getRequestURI());
    }

    private UserBusinessException unauthorized() {
        return new UserBusinessException(HttpStatus.UNAUTHORIZED, ApiCode.UNAUTHORIZED, "Unauthorized");
    }

    private UserBusinessException forbidden() {
        return new UserBusinessException(HttpStatus.FORBIDDEN, ApiCode.FORBIDDEN, "Forbidden");
    }
}
