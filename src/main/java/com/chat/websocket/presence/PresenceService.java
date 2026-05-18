package com.chat.websocket.presence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class PresenceService {

    private static final String KEY_PREFIX = "user:online:";
    private static final Duration ONLINE_TTL = Duration.ofSeconds(60);

    private final StringRedisTemplate stringRedisTemplate;

    public void heartbeat(String userId) {
        stringRedisTemplate.opsForValue().set(KEY_PREFIX + userId, "1", ONLINE_TTL);
        log.debug("[PRESENCE] userId={}", userId);
    }

    public boolean isOnline(String userId) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(KEY_PREFIX + userId));
    }

    public void offline(String userId) {
        stringRedisTemplate.delete(KEY_PREFIX + userId);
        log.debug("[PRESENCE_OFFLINE] userId={}", userId);
    }
}
