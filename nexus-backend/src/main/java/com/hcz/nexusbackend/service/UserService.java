package com.hcz.nexusbackend.service;

import com.hcz.nexusbackend.entity.User;
import com.hcz.nexusbackend.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    public String register(String username, String password) {
        User existing = userMapper.findByUsername(username);
        if (existing != null) {
            return "用户名已存在";
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        userMapper.insert(user);
        return "注册成功";
    }

    public String login(String username, String password) {
        User user = userMapper.findByUsername(username);
        if (user == null || !user.getPassword().equals(password)) {
            return "账号或密码错误";
        }
        return "登录成功";
    }
}
