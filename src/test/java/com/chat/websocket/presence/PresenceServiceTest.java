package com.chat.websocket.presence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class PresenceServiceTest {

    @Mock StringRedisTemplate stringRedisTemplate;
    @Mock ValueOperations<String, String> valueOps;
    @InjectMocks PresenceService presenceService;

    @Test
    void heartbeat_Redis에_TTL_60초로_저장() {
        // given
        given(stringRedisTemplate.opsForValue()).willReturn(valueOps);

        // when
        presenceService.heartbeat("user-a");

        // then
        then(valueOps).should().set("user:online:user-a", "1", Duration.ofSeconds(60));
    }

    @Test
    void isOnline_키가_있으면_true() {
        // given
        given(stringRedisTemplate.hasKey("user:online:user-a")).willReturn(true);

        // when & then
        assertThat(presenceService.isOnline("user-a")).isTrue();
    }

    @Test
    void isOnline_키가_없으면_false() {
        // given
        given(stringRedisTemplate.hasKey("user:online:user-b")).willReturn(null);

        // when & then
        assertThat(presenceService.isOnline("user-b")).isFalse();
    }
}
