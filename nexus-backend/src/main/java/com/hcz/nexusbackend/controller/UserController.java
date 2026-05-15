package com.hcz.nexusbackend.controller;

import com.hcz.nexusbackend.common.Result;
import com.hcz.nexusbackend.entity.User;
import com.hcz.nexusbackend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public Result<Void> register(@RequestParam("username") String username, @RequestParam("password") String password) {
        userService.register(username, password);
        return Result.success("注册成功", null);
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestParam("username") String username, @RequestParam("password") String password) {
        return Result.success(userService.login(username, password));
    }

    @GetMapping("/detail")
    public Result<User> detail(@RequestParam("id") Long id) {
        return Result.success(userService.getById(id));
    }
}
