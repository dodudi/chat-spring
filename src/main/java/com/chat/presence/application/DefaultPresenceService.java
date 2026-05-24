package com.chat.presence.application;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class DefaultPresenceService implements PresenceService {

    private final StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "user:online:";
    private static final Duration ONLINE_TTL = Duration.ofSeconds(60);

    @Override
    public void heartbeat(String userId) {
        redisTemplate.opsForValue().set(KEY_PREFIX + userId, "1", ONLINE_TTL);
    }

    @Override
    public void offline(String userId) {
        redisTemplate.delete(KEY_PREFIX + userId);
    }

    @Override
    public boolean isOnline(String userId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + userId));
    }
}
