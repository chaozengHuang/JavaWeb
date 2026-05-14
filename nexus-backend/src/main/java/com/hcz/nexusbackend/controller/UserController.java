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
    public Result<String> register(@RequestParam String username, @RequestParam String password) {
        String result = userService.register(username, password);
        return Result.success(result);
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestParam String username, @RequestParam String password) {
        Map<String, Object> result = userService.login(username, password);
        return Result.success("登录成功", result);
    }

    @PostMapping("/updateProfile")
    public Result<String> updateProfile(@RequestParam Integer id,
                                         @RequestParam String phone,
                                         @RequestParam String jobNature,
                                         @RequestParam String workLocation) {
        String result = userService.updateProfile(id, phone, jobNature, workLocation);
        return Result.success(result);
    }

    @GetMapping("/detail")
    public Result<User> detail(@RequestParam Integer id) {
        User user = userService.getById(id);
        return Result.success(user);
    }
}
