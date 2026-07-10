package com.nursing.user.interceptor;

import com.nursing.user.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class UserTokenInterceptor implements HandlerInterceptor {
    private static final String LOGOUT_PATH = "/api/v1/users/logout";

    private final TokenService tokenService;

    public UserTokenInterceptor(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String token = tokenService.resolveBearerToken(request.getHeader("Authorization"));
        TokenService.TokenPayload payload = isLogoutRequest(request)
                ? tokenService.validateTokenForLogout(token)
                : tokenService.validateToken(token);
        // 后续 Controller 通过 request attribute 获取可信用户身份，不信任客户端自传 userId。
        request.setAttribute("userId", payload.getUserId());
        request.setAttribute("token", token);
        return true;
    }

    private boolean isLogoutRequest(HttpServletRequest request) {
        return LOGOUT_PATH.equals(request.getRequestURI());
    }
}
