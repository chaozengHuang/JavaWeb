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

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private SmsService smsService;

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
        // 发送欢迎消息
        try {
            Long notifierId = notificationService.getNotifyUserId();
            if (notifierId != null) {
                messageService.send(notifierId, user.getId(),
                        dto.getUsername() + "你好，欢迎来到Nexus贴吧");
            }
        } catch (Exception ignored) {}
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

    // ==================== 忘记密码 ====================

    public Map<String, Object> sendResetCode(String username) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) {
            throw new BusinessException(404, "该用户不存在");
        }
        if (user.getPhone() == null || user.getPhone().isEmpty()) {
            throw new BusinessException(400, "PHONE_REQUIRED:用户 " + username + " 未绑定手机号，请联系管理员，手机号：19219785122");
        }
        boolean sent = smsService.sendVerifyCode(user.getPhone());
        if (!sent) {
            throw new BusinessException("短信发送失败，请稍后重试");
        }
        log.info("找回密码验证码已发送: username={}, phone={}", username, user.getPhone());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "验证码已发送，10分钟内有效");
        return result;
    }

    public Map<String, Object> verifyAndReset(String username, String code) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) {
            throw new BusinessException(404, "该用户不存在");
        }
        if (user.getPhone() == null || user.getPhone().isEmpty()) {
            throw new BusinessException(400, "PHONE_REQUIRED:用户 " + username + " 未绑定手机号，请联系管理员，手机号：19219785122");
        }
        if (!smsService.verifyCode(user.getPhone(), code)) {
            throw new BusinessException("验证码错误或已过期");
        }
        // 重置密码为123456
        user.setPassword(passwordEncoder.encode("123456"));
        userMapper.updateById(user);
        log.info("用户 {} 通过短信验证重置了密码", username);
        // 通知管理员
        try {
            Long notifierId = notificationService.getNotifyUserId();
            if (notifierId != null) {
                // 通知所有 SYS_ADMIN
                java.util.List<User> admins = userMapper.selectList(
                        new LambdaQueryWrapper<User>().eq(User::getGlobalRole, "SYS_ADMIN"));
                for (User admin : admins) {
                    messageService.send(notifierId, admin.getId(), username + " 通过短信验证重置了密码");
                }
            }
        } catch (Exception ignored) {}
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "密码已重置为123456，请登录后尽快修改");
        return result;
    }
}
