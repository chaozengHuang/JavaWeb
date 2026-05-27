package com.hcz.nexusbackend.interceptor;

import com.hcz.nexusbackend.annotation.RequireAdmin;
import com.hcz.nexusbackend.exception.BusinessException;
import com.hcz.nexusbackend.util.SecurityUtils;
import org.springframework.lang.NonNull;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class AdminInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        RequireAdmin classAnnotation = handlerMethod.getBeanType().getAnnotation(RequireAdmin.class);
        RequireAdmin methodAnnotation = handlerMethod.getMethodAnnotation(RequireAdmin.class);

        if (classAnnotation == null && methodAnnotation == null) {
            return true;
        }

        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }

        String role = SecurityUtils.getGlobalRole();
        if (!"SYS_ADMIN".equals(role)) {
            throw new BusinessException(403, "权限不足：需要管理员权限");
        }

        return true;
    }
}
