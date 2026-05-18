package com.chat.websocket.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class JwtChannelInterceptorTest {

    @Mock JwtDecoder jwtDecoder;
    @Mock MessageChannel channel;
    @InjectMocks JwtChannelInterceptor interceptor;

    @Test
    void preSend_CONNECT가_아니면_그대로_통과() {
        // given
        Message<?> message = buildStompMessage(StompCommand.SEND, null);

        // when
        Message<?> result = interceptor.preSend(message, channel);

        // then
        assertThat(result).isEqualTo(message);
        then(jwtDecoder).shouldHaveNoInteractions();
    }

    @Test
    void preSend_CONNECT_토큰_없으면_MessageDeliveryException() {
        // given
        Message<?> message = buildStompMessage(StompCommand.CONNECT, null);

        // when & then
        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(MessageDeliveryException.class)
                .hasMessageContaining("Missing authorization token");
    }

    @Test
    void preSend_CONNECT_유효하지_않은_토큰이면_MessageDeliveryException() {
        // given
        Message<?> message = buildStompMessage(StompCommand.CONNECT, "Bearer bad-token");
        given(jwtDecoder.decode("bad-token")).willThrow(new JwtException("bad token"));

        // when & then
        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(MessageDeliveryException.class)
                .hasMessageContaining("Invalid JWT");
    }

    @Test
    void preSend_CONNECT_유효한_토큰이면_Principal_세팅() {
        // given
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("user-a")
                .claim("iss", "http://localhost")
                .build();
        Message<?> message = buildStompMessage(StompCommand.CONNECT, "Bearer token");
        given(jwtDecoder.decode("token")).willReturn(jwt);

        // when
        Message<?> result = interceptor.preSend(message, channel);

        // then
        assertThat(result).isNotNull();
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(result);
        assertThat(accessor.getUser()).isInstanceOf(JwtPrincipal.class);
        assertThat(accessor.getUser().getName()).isEqualTo("user-a");
    }

    private Message<?> buildStompMessage(StompCommand command, String authHeader) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        if (authHeader != null) {
            accessor.setNativeHeader("Authorization", authHeader);
        }
        accessor.setSessionId("session-1");
        accessor.setNativeHeader("heart-beat", "0,0");
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
