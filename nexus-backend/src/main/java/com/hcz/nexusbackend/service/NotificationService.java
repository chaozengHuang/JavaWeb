package com.hcz.nexusbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hcz.nexusbackend.entity.User;
import com.hcz.nexusbackend.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class NotificationService {

    private static final String NOTIFY_USERNAME = "系统通知";
    private static final String NOTIFY_ROLE = "NOTIFY_ADMIN";

    @Autowired
    private UserMapper userMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private String plainPassword;

    @PostConstruct
    public void init() {
        User existing = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, NOTIFY_USERNAME));
        // 每次启动都重新生成密码
        plainPassword = UUID.randomUUID().toString().substring(0, 12);
        if (existing == null) {
            User notify = new User();
            notify.setUsername(NOTIFY_USERNAME);
            notify.setPassword(passwordEncoder.encode(plainPassword));
            notify.setGlobalRole(NOTIFY_ROLE);
            notify.setPoints(0);
            notify.setStatus("ACTIVE");
            notify.setBio("系统通知账号，由所有系统管理员共同运营");
            userMapper.insert(notify);
            log.info("通知管理员已创建: username={}, password={}", NOTIFY_USERNAME, plainPassword);
        } else {
            // 每次重启重置密码
            existing.setPassword(passwordEncoder.encode(plainPassword));
            if (!NOTIFY_ROLE.equals(existing.getGlobalRole())) {
                existing.setGlobalRole(NOTIFY_ROLE);
            }
            userMapper.updateById(existing);
            log.info("通知管理员密码已重置: username={}, password={}", NOTIFY_USERNAME, plainPassword);
        }
    }

    public Long getNotifyUserId() {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, NOTIFY_USERNAME));
        if (user == null) {
            init();
            user = userMapper.selectOne(
                    new LambdaQueryWrapper<User>().eq(User::getUsername, NOTIFY_USERNAME));
        }
        return user != null ? user.getId() : null;
    }

    public Map<String, Object> getNotifyCredentials() {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, NOTIFY_USERNAME));
        Map<String, Object> result = new LinkedHashMap<>();
        if (user != null) {
            result.put("username", user.getUsername());
            result.put("password", plainPassword);
            result.put("userId", user.getId());
            result.put("role", user.getGlobalRole());
        }
        return result;
    }
}
