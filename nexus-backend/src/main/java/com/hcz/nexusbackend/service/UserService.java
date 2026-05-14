package com.hcz.nexusbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hcz.nexusbackend.entity.User;
import com.hcz.nexusbackend.exception.BusinessException;
import com.hcz.nexusbackend.mapper.UserMapper;
import com.hcz.nexusbackend.util.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    public String register(String username, String password) {
        User existing = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (existing != null) {
            throw new BusinessException("用户名已存在");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setPoints(0);
        user.setGlobalRole("USER");
        user.setStatus("NORMAL");
        userMapper.insert(user);
        return "注册成功";
    }

    public Map<String, Object> login(String username, String password) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null || !user.getPassword().equals(password)) {
            throw new BusinessException("账号或密码错误");
        }
        String token = JwtUtils.generate(user.getId(), user.getGlobalRole());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token", token);
        result.put("user", user);
        return result;
    }

    public User getById(Integer id) {
        return userMapper.selectById(id);
    }

    public String updateProfile(Integer id, String phone, String jobNature, String workLocation) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        User update = new User();
        update.setId(Long.valueOf(id));
        update.setPhone(phone);
        update.setJobNature(jobNature);
        update.setLocation(workLocation);
        userMapper.updateById(update);
        return "更新成功";
    }
}
