package com.hcz.nexusbackend.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hcz.nexusbackend.service.MessageService;
import lombok.extern.slf4j.Slf4j;
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
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final MessageService messageService;

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
