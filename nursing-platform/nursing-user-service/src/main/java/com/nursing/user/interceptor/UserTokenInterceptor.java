package com.nursing.user.interceptor;

import com.nursing.user.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class UserTokenInterceptor implements HandlerInterceptor {

    private final TokenService tokenService;

    public UserTokenInterceptor(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String token = tokenService.resolveBearerToken(request.getHeader("Authorization"));
        TokenService.TokenPayload payload = tokenService.validateToken(token);
        request.setAttribute("userId", payload.getUserId());
        request.setAttribute("token", token);
        return true;
    }
}
