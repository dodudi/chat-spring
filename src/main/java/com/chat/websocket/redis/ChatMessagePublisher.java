package com.chat.websocket.redis;

import com.chat.websocket.dto.ChatBroadcastMessage;
import com.chat.websocket.dto.NotificationMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatMessagePublisher {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public void publish(Long roomId, ChatBroadcastMessage message) {
        try {
            stringRedisTemplate.convertAndSend(
                    "pubsub:room:" + roomId,
                    objectMapper.writeValueAsString(message));
        } catch (JsonProcessingException e) {
            log.error("[REDIS_PUB_FAIL] roomId={}", roomId, e);
        }
    }

    public void publishNotification(String userId, NotificationMessage notification) {
        try {
            stringRedisTemplate.convertAndSend(
                    "pubsub:user:" + userId,
                    objectMapper.writeValueAsString(notification));
        } catch (JsonProcessingException e) {
            log.error("[REDIS_PUB_FAIL] userId={}", userId, e);
        }
    }
}
