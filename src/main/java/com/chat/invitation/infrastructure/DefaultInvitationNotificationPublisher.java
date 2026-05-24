package com.chat.invitation.infrastructure;

import com.chat.invitation.application.InvitationNotificationPublisher;
import com.chat.invitation.dto.InvitationResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultInvitationNotificationPublisher implements InvitationNotificationPublisher {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void publish(String userId, InvitationResponse invitation) {
        try {
            redisTemplate.convertAndSend("pubsub:user:" + userId,
                    objectMapper.writeValueAsString(invitation));
        } catch (JsonProcessingException e) {
            log.error("[REDIS_PUB_FAIL] userId={}", userId, e);
        } catch (Exception e) {
            log.error("[REDIS_PUB_FAIL] userId={}", userId, e);
        }
    }
}
