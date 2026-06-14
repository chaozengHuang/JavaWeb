package com.hcz.nexusbackend.controller;

import com.hcz.nexusbackend.common.Result;
import com.hcz.nexusbackend.dto.UserLoginDTO;
import com.hcz.nexusbackend.dto.UserRegisterDTO;
import com.hcz.nexusbackend.entity.User;
import com.hcz.nexusbackend.service.SmsService;
import com.hcz.nexusbackend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private SmsService smsService;

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody UserRegisterDTO dto) {
        userService.register(dto);
        return Result.success("注册成功", null);
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody UserLoginDTO dto) {
        return Result.success(userService.login(dto));
    }

    @GetMapping("/info")
    public Result<User> info() {
        return Result.success(userService.getCurrentUser());
    }

    @GetMapping("/{userId}")
    public Result<User> getUserById(@PathVariable Long userId) {
        return Result.success(userService.getById(userId));
    }

    // ==================== 忘记密码 ====================

    @PostMapping("/forgot-password/send-code")
    public Result<Map<String, Object>> sendForgotPasswordCode(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        Map<String, Object> result = userService.sendResetCode(username);
        return Result.success(result);
    }

    @PostMapping("/forgot-password/verify")
    public Result<Map<String, Object>> verifyAndReset(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String code = body.get("code");
        Map<String, Object> result = userService.verifyAndReset(username, code);
        return Result.success(result);
    }
}
