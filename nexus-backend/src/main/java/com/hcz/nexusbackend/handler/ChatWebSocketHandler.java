package com.hcz.nexusbackend.handler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hcz.nexusbackend.entity.FriendRelation;
import com.hcz.nexusbackend.mapper.FriendRelationMapper;
import com.hcz.nexusbackend.mapper.MessageMapper;
import com.hcz.nexusbackend.service.MessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Map<Long, WebSocketSession> ONLINE_USERS = new ConcurrentHashMap<>();
    private static final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private final MessageService messageService;

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private FriendRelationMapper friendRelationMapper;

    public ChatWebSocketHandler(MessageService messageService) {
        this.messageService = messageService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            ONLINE_USERS.put(userId, session);
            log.info("WebSocket 连接建立: userId={}", userId);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage textMessage) throws Exception {
        Long senderId = (Long) session.getAttributes().get("userId");
        if (senderId == null) return;

        Map<String, Object> payload = objectMapper.readValue(textMessage.getPayload(), Map.class);
        Long receiverId = Long.valueOf(payload.get("receiverId").toString());
        String content = (String) payload.get("content");

        // 检查是否为好友
        Long friendCount = friendRelationMapper.selectCount(
                new LambdaQueryWrapper<FriendRelation>()
                        .eq(FriendRelation::getUserId, senderId)
                        .eq(FriendRelation::getFriendId, receiverId)
                        .eq(FriendRelation::getStatus, "ACCEPTED"));
        boolean isFriend = friendCount > 0;

        if (!isFriend) {
            // 非好友限制：每人只能发1条
            Long sentCount = messageMapper.selectCount(
                    new LambdaQueryWrapper<com.hcz.nexusbackend.entity.Message>()
                            .eq(com.hcz.nexusbackend.entity.Message::getSenderId, senderId)
                            .eq(com.hcz.nexusbackend.entity.Message::getReceiverId, receiverId));
            if (sentCount >= 1) {
                // 拒绝发送
                Map<String, Object> reject = new java.util.LinkedHashMap<>();
                reject.put("type", "chat_blocked");
                reject.put("message", "您只能向非好友发送一条消息，请添加好友后继续私聊");
                sendMessage(session, objectMapper.writeValueAsString(reject));
                return;
            }
        }

        // 保存消息到数据库
        var msg = messageService.send(senderId, receiverId, content);

        // 构建推送消息
        Map<String, Object> pushMsg = new java.util.LinkedHashMap<>();
        pushMsg.put("type", "new_message");
        pushMsg.put("message", msg);

        String pushJson = objectMapper.writeValueAsString(pushMsg);

        // 推送给接收者（如果在线）
        WebSocketSession receiverSession = ONLINE_USERS.get(receiverId);
        if (receiverSession != null && receiverSession.isOpen()) {
            sendMessage(receiverSession, pushJson);
        }

        // 也给发送者回执
        Map<String, Object> ack = new java.util.LinkedHashMap<>();
        ack.put("type", "message_sent");
        ack.put("message", msg);
        sendMessage(session, objectMapper.writeValueAsString(ack));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            ONLINE_USERS.remove(userId);
            log.info("WebSocket 连接关闭: userId={}", userId);
        }
    }

    private void sendMessage(WebSocketSession session, String message) {
        try {
            if (session.isOpen()) {
                synchronized (session) {
                    session.sendMessage(new TextMessage(message));
                }
            }
        } catch (IOException e) {
            log.error("WebSocket 消息发送失败", e);
        }
    }

    public static boolean isUserOnline(Long userId) {
        WebSocketSession session = ONLINE_USERS.get(userId);
        return session != null && session.isOpen();
    }
}
