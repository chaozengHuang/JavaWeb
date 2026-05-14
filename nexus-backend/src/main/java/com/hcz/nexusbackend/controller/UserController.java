package com.hcz.nexusbackend.controller;

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
    public String register(@RequestParam("username") String username, @RequestParam("password") String password) {
        userService.register(username, password);
        return "注册成功";
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestParam("username") String username, @RequestParam("password") String password) {
        return userService.login(username, password);
    }

    @GetMapping("/detail")
    public User detail(@RequestParam("id") Long id) {
        return userService.getById(id);
    }
}
