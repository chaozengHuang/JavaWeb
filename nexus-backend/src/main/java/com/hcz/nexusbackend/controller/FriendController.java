package com.hcz.nexusbackend.controller;

import com.hcz.nexusbackend.common.Result;
import com.hcz.nexusbackend.service.FriendService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/friend")
public class FriendController {

    @Autowired
    private FriendService friendService;

    @PostMapping("/request/{userId}")
    public Result<Void> sendRequest(@PathVariable Long userId) {
        friendService.sendRequest(userId);
        return Result.success();
    }

    @PutMapping("/accept/{requestId}")
    public Result<Void> acceptRequest(@PathVariable Long requestId) {
        friendService.acceptRequest(requestId);
        return Result.success();
    }

    @DeleteMapping("/reject/{requestId}")
    public Result<Void> rejectRequest(@PathVariable Long requestId) {
        friendService.rejectRequest(requestId);
        return Result.success();
    }

    @DeleteMapping("/{friendId}")
    public Result<Void> deleteFriend(@PathVariable Long friendId) {
        friendService.deleteFriend(friendId);
        return Result.success();
    }

    @GetMapping("/list")
    public Result<List<Map<String, Object>>> getFriendList() {
        return Result.success(friendService.getFriendList());
    }

    @GetMapping("/requests")
    public Result<List<Map<String, Object>>> getPendingRequests() {
        return Result.success(friendService.getPendingRequests());
    }

    @GetMapping("/requests/count")
    public Result<Map<String, Object>> getPendingCount() {
        return Result.success(friendService.getPendingCount());
    }

    @GetMapping("/status/{userId}")
    public Result<Map<String, Object>> getFriendStatus(@PathVariable Long userId) {
        String status = friendService.getFriendStatus(userId);
        return Result.success(Map.of("status", status));
    }
}
