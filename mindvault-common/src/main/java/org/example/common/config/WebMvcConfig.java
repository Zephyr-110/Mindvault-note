package org.example.common.config;

import lombok.RequiredArgsConstructor;
import org.example.common.logincheck.LoginInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final LoginInterceptor loginInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                //指定要拦截的请求
                .addPathPatterns("/api/**")
                //排除不拦截的路径
                .excludePathPatterns(
                        "/api/user/login",
                        "/api/user/register"
                );
    }
}
//用于登录校验