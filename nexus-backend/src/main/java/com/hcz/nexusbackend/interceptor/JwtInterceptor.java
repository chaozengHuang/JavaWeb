package com.hcz.nexusbackend.interceptor;

import com.hcz.nexusbackend.entity.User;
import com.hcz.nexusbackend.exception.BusinessException;
import com.hcz.nexusbackend.mapper.UserMapper;
import com.hcz.nexusbackend.util.JwtUtils;
import com.hcz.nexusbackend.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private UserMapper userMapper;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                Map<String, Object> claims = JwtUtils.parse(token);
                Long userId = (Long) claims.get("userId");
                if (userId != null) {
                    User user = userMapper.selectById(userId);
                    if (user == null || "BANNED".equals(user.getStatus())) {
                        throw new BusinessException(403, "账号已被封禁，请联系管理员");
                    }
                }
                SecurityUtils.set(claims);
            } catch (BusinessException e) {
                throw e;
            } catch (Exception ignored) {
                // token 无效，继续但不设置用户信息
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request,
                                @NonNull HttpServletResponse response,
                                @NonNull Object handler,
                                Exception ex) {
        SecurityUtils.clear();
    }
}
