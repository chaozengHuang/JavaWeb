package com.hcz.nexusbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hcz.nexusbackend.dto.UserLoginDTO;
import com.hcz.nexusbackend.dto.UserRegisterDTO;
import com.hcz.nexusbackend.entity.User;
import com.hcz.nexusbackend.exception.BusinessException;
import com.hcz.nexusbackend.mapper.UserMapper;
import com.hcz.nexusbackend.util.JwtUtils;
import com.hcz.nexusbackend.util.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public void register(UserRegisterDTO dto) {
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessException("两次输入的密码不一致");
        }
        User existing = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (existing != null) {
            throw new BusinessException("用户名已存在");
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        userMapper.insert(user);
        log.info("新用户注册: {}", user.getUsername());
    }

    public Map<String, Object> login(UserLoginDTO dto) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(401, "账号或密码错误");
        }
        if ("BANNED".equals(user.getStatus())) {
            throw new BusinessException(403, "账号已被封禁，请联系管理员");
        }
        String token = JwtUtils.generate(user.getId(), user.getGlobalRole());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token", token);
        result.put("user", user);
        log.info("用户登录: {}", user.getUsername());
        return result;
    }

    public User getById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }

    public User getCurrentUser() {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        return getById(userId);
    }
}
