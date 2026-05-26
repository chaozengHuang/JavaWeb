package com.hcz.nexusbackend.controller;

import com.hcz.nexusbackend.common.Result;
import com.hcz.nexusbackend.entity.Message;
import com.hcz.nexusbackend.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/message")
public class MessageController {

    @Autowired
    private MessageService messageService;

    @GetMapping("/history/{userId}")
    public Result<List<Message>> getHistory(@PathVariable Long userId) {
        return Result.success(messageService.getHistory(userId));
    }

    @PutMapping("/read/{userId}")
    public Result<Void> markAsRead(@PathVariable Long userId) {
        messageService.markAsRead(userId);
        return Result.success();
    }

    @GetMapping("/unread")
    public Result<Map<String, Object>> getUnreadCount() {
        return Result.success(messageService.getUnreadCountMap());
    }

    @GetMapping("/chat-list")
    public Result<List<Map<String, Object>>> getChatList() {
        return Result.success(messageService.getChatList());
    }
}
