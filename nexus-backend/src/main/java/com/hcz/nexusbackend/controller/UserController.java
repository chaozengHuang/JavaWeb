package com.hcz.nexusbackend.controller;

import com.hcz.nexusbackend.entity.User;
import com.hcz.nexusbackend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public String register(@RequestParam String username, @RequestParam String password) {
        return userService.register(username, password);
    }

    @PostMapping("/login")
    public User login(@RequestParam String username, @RequestParam String password) {
        return userService.login(username, password);
    }

    @PostMapping("/updateProfile")
    public String updateProfile(@RequestParam Integer id,
                                @RequestParam String phone,
                                @RequestParam String jobNature,
                                @RequestParam String workLocation) {
        return userService.updateProfile(id, phone, jobNature, workLocation);
    }

    @GetMapping("/detail")
    public User detail(@RequestParam Integer id) {
        return userService.getById(id);
    }
}
