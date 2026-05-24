package com.chat.message.infrastructure;

import com.chat.message.application.ChatMessagePublisher;
import com.chat.message.dto.MessageResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultChatMessagePublisher implements ChatMessagePublisher {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void publishToRoom(UUID roomId, MessageResponse message) {
        publishEventToRoom(roomId, message);
    }

    @Override
    public void publishEventToRoom(UUID roomId, Object payload) {
        try {
            redisTemplate.convertAndSend("pubsub:room:" + roomId,
                    objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            log.error("[REDIS_PUB_FAIL] roomId={}", roomId, e);
        } catch (Exception e) {
            log.error("[REDIS_PUB_FAIL] roomId={}", roomId, e);
        }
    }
}
