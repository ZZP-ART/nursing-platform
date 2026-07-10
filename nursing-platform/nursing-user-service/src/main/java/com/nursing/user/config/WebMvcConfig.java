package com.nursing.user.config;

import com.nursing.user.interceptor.UserTokenInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final UserTokenInterceptor userTokenInterceptor;

    public WebMvcConfig(UserTokenInterceptor userTokenInterceptor) {
        this.userTokenInterceptor = userTokenInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 只拦截需要登录态的用户接口，注册、登录、短信验证码和重置密码保持公开。
        registry.addInterceptor(userTokenInterceptor)
                .addPathPatterns(
                        "/api/v1/users/profile",
                        "/api/v1/users/logout",
                        "/api/v1/files/upload");
    }
}
