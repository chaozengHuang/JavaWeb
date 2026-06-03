package com.hcz.nexusbackend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.hcz.nexusbackend.entity.Message;
import com.hcz.nexusbackend.entity.User;
import com.hcz.nexusbackend.exception.BusinessException;
import com.hcz.nexusbackend.mapper.MessageMapper;
import com.hcz.nexusbackend.mapper.UserMapper;
import com.hcz.nexusbackend.util.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class MessageService {

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private UserMapper userMapper;

    public Message send(Long senderId, Long receiverId, String content) {
        Message msg = new Message();
        msg.setSenderId(senderId);
        msg.setReceiverId(receiverId);
        msg.setContent(content);
        msg.setType(1);
        msg.setStatus(0);
        messageMapper.insert(msg);
        log.info("消息发送: {} -> {}", senderId, receiverId);
        return msg;
    }

    public List<Map<String, Object>> getHistory(Long otherUserId) {
        Long currentUserId = getCurrentUserId();
        List<Message> messages = messageMapper.selectHistory(currentUserId, otherUserId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Message msg : messages) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", msg.getId());
            map.put("senderId", msg.getSenderId());
            map.put("receiverId", msg.getReceiverId());
            map.put("content", msg.getContent());
            map.put("type", msg.getType());
            map.put("status", msg.getStatus());
            map.put("createTime", msg.getCreateTime().toString().replace("T", " "));
            User sender = userMapper.selectById(msg.getSenderId());
            if (sender != null) {
                map.put("senderAvatar", sender.getAvatar());
                map.put("senderUsername", sender.getUsername());
            }
            result.add(map);
        }
        return result;
    }

    public void markAsRead(Long senderId) {
        Long currentUserId = getCurrentUserId();
        messageMapper.markAsRead(senderId, currentUserId);
    }

    public int getUnreadCount() {
        Long currentUserId = getCurrentUserId();
        return messageMapper.countUnread(currentUserId);
    }

    public Map<String, Object> getUnreadCountMap() {
        Long currentUserId = getCurrentUserId();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("count", messageMapper.countUnread(currentUserId));
        return result;
    }

    public List<Map<String, Object>> getChatList() {
        Long currentUserId = getCurrentUserId();
        List<Long> chatUserIds = messageMapper.selectChatUserIds(currentUserId);
        List<Map<String, Object>> chatList = new ArrayList<>();
        for (Long otherId : chatUserIds) {
            User other = userMapper.selectById(otherId);
            if (other == null) continue;
            Map<String, Object> item = new HashMap<>();
            item.put("userId", other.getId());
            item.put("username", other.getUsername());
            item.put("avatar", other.getAvatar());
            item.put("unread", messageMapper.countUnreadBySender(currentUserId, otherId));
            chatList.add(item);
        }
        return chatList;
    }

    private Long getCurrentUserId() {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        return userId;
    }
}
