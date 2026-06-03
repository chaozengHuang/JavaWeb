package com.hcz.nexusbackend.controller;

import com.hcz.nexusbackend.common.Result;
import com.hcz.nexusbackend.service.PointsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/points")
public class PointsController {

    @Autowired
    private PointsService pointsService;

    @PostMapping("/check-in")
    public Result<Map<String, Object>> checkIn() {
        return Result.success("签到成功", pointsService.checkIn());
    }

    @GetMapping("/check-in/status")
    public Result<Map<String, Object>> getCheckInStatus() {
        return Result.success(pointsService.getCheckInStatus());
    }

    @PostMapping("/recharge")
    public Result<Map<String, Object>> recharge(@RequestBody Map<String, Object> body) {
        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        return Result.success("充值成功", pointsService.recharge(amount));
    }

    @GetMapping("/recharge/list")
    public Result<Map<String, Object>> getRechargeList(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size) {
        return Result.success(pointsService.getRechargeList(page, size));
    }
}
