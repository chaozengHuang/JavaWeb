package com.hcz.nexusbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.hcz.nexusbackend.entity.DailyCheckIn;
import com.hcz.nexusbackend.entity.RechargeOrder;
import com.hcz.nexusbackend.entity.User;
import com.hcz.nexusbackend.exception.BusinessException;
import com.hcz.nexusbackend.mapper.DailyCheckInMapper;
import com.hcz.nexusbackend.mapper.RechargeOrderMapper;
import com.hcz.nexusbackend.mapper.UserMapper;
import com.hcz.nexusbackend.util.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class PointsService {

    @Autowired
    private DailyCheckInMapper checkInMapper;

    @Autowired
    private RechargeOrderMapper rechargeOrderMapper;

    @Autowired
    private UserMapper userMapper;

    private static final Random RANDOM = new Random();

    private Long getCurrentUserId() {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        return userId;
    }

    @Transactional
    public Map<String, Object> checkIn() {
        Long userId = getCurrentUserId();
        LocalDate today = LocalDate.now();

        // 检查今日是否已签到
        Long count = checkInMapper.selectCount(
                new LambdaQueryWrapper<DailyCheckIn>()
                        .eq(DailyCheckIn::getUserId, userId)
                        .eq(DailyCheckIn::getCheckInDate, today));
        if (count > 0) {
            throw new BusinessException("今日已签到");
        }

        // 随机 5-15 积分
        int points = 5 + RANDOM.nextInt(11);

        // 插入签到记录
        DailyCheckIn checkIn = new DailyCheckIn();
        checkIn.setUserId(userId);
        checkIn.setCheckInDate(today);
        checkIn.setPointsAwarded(points);
        checkInMapper.insert(checkIn);

        // 增加用户积分
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setPoints((user.getPoints() != null ? user.getPoints() : 0) + points);
        userMapper.updateById(user);

        log.info("用户 {} 签到成功，获得 {} 积分", userId, points);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("pointsAwarded", points);
        result.put("totalPoints", user.getPoints());
        return result;
    }

    public Map<String, Object> getCheckInStatus() {
        Long userId = getCurrentUserId();
        LocalDate today = LocalDate.now();

        Long count = checkInMapper.selectCount(
                new LambdaQueryWrapper<DailyCheckIn>()
                        .eq(DailyCheckIn::getUserId, userId)
                        .eq(DailyCheckIn::getCheckInDate, today));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("checkedIn", count > 0);

        // 获取最近7天签到记录
        LocalDate weekAgo = today.minusDays(7);
        List<DailyCheckIn> recent = checkInMapper.selectList(
                new LambdaQueryWrapper<DailyCheckIn>()
                        .eq(DailyCheckIn::getUserId, userId)
                        .ge(DailyCheckIn::getCheckInDate, weekAgo)
                        .orderByDesc(DailyCheckIn::getCheckInDate));
        result.put("recentRecords", recent);

        return result;
    }

    @Transactional
    public Map<String, Object> recharge(BigDecimal amount) {
        Long userId = getCurrentUserId();

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("充值金额必须大于0");
        }

        // 1元 = 10积分
        int points = amount.intValue() * 10;

        // 生成订单号
        String orderNo = "RC" + System.currentTimeMillis() + "_" + userId + "_" + RANDOM.nextInt(10000);

        // 直接完成充值
        RechargeOrder order = new RechargeOrder();
        order.setUserId(userId);
        order.setOrderNo(orderNo);
        order.setAmount(amount);
        order.setPoints(points);
        order.setStatus("COMPLETED");
        order.setCompletedAt(LocalDateTime.now());
        rechargeOrderMapper.insert(order);

        // 增加用户积分
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setPoints((user.getPoints() != null ? user.getPoints() : 0) + points);
        userMapper.updateById(user);

        log.info("用户 {} 充值 {} 元，获得 {} 积分", userId, amount, points);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orderNo", orderNo);
        result.put("amount", amount);
        result.put("points", points);
        result.put("totalPoints", user.getPoints());
        return result;
    }

    public Map<String, Object> getRechargeList(Integer page, Integer size) {
        Long userId = getCurrentUserId();
        int pageNum = page != null ? page : 1;
        int pageSize = size != null ? size : 10;

        Page<RechargeOrder> pageParam = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<RechargeOrder> wrapper = new LambdaQueryWrapper<RechargeOrder>()
                .eq(RechargeOrder::getUserId, userId)
                .eq(RechargeOrder::getStatus, "COMPLETED")
                .orderByDesc(RechargeOrder::getCreatedAt);

        Page<RechargeOrder> result = rechargeOrderMapper.selectPage(pageParam, wrapper);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        data.put("page", pageNum);
        data.put("size", pageSize);
        return data;
    }
}
