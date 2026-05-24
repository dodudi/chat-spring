package com.chat.profile.infrastructure;

import com.chat.common.RoomEvent;
import com.chat.profile.application.ProfileEventPublisher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultProfileEventPublisher implements ProfileEventPublisher {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void publish(List<UUID> roomIds, RoomEvent event) {
        String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            log.error("[REDIS_PUB_FAIL] event serialization failed event={}", event.getClass().getSimpleName(), e);
            return;
        }
        for (UUID roomId : roomIds) {
            try {
                redisTemplate.convertAndSend("pubsub:room:" + roomId, payload);
            } catch (Exception e) {
                log.error("[REDIS_PUB_FAIL] roomId={}", roomId, e);
            }
        }
    }
}
