package com.chat.websocket.redis;

import com.chat.websocket.dto.ChatBroadcastMessage;
import com.chat.websocket.dto.NotificationMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatMessageSubscriber implements MessageListener {

    private static final String ROOM_PREFIX = "pubsub:room:";
    private static final String USER_PREFIX = "pubsub:user:";

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        try {
            if (channel.startsWith(ROOM_PREFIX)) {
                Long roomId = Long.parseLong(channel.substring(ROOM_PREFIX.length()));
                ChatBroadcastMessage broadcast = objectMapper.readValue(body, ChatBroadcastMessage.class);
                messagingTemplate.convertAndSend("/topic/rooms/" + roomId, broadcast);
            } else if (channel.startsWith(USER_PREFIX)) {
                String userId = channel.substring(USER_PREFIX.length());
                NotificationMessage notification = objectMapper.readValue(body, NotificationMessage.class);
                messagingTemplate.convertAndSendToUser(userId, "/queue/notifications", notification);
            }
        } catch (Exception e) {
            log.error("[REDIS_SUB_FAIL] channel={}", channel, e);
        }
    }
}
