package com.hcz.nexusbackend.config;

import com.hcz.nexusbackend.interceptor.AdminInterceptor;
import com.hcz.nexusbackend.interceptor.JwtInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class JwtInterceptorConfig implements WebMvcConfigurer {

    @Autowired
    private JwtInterceptor jwtInterceptor;

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/user/login", "/user/register",
                        "/api/user/login", "/api/user/register"
                );

        registry.addInterceptor(new AdminInterceptor())
                .addPathPatterns("/admin/**");
    }
}
