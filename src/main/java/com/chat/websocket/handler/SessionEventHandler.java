package com.chat.websocket.handler;

import com.chat.presence.application.PresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionEventHandler {

    private final PresenceService presenceService;

    @EventListener
    public void handleConnect(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        if (accessor.getUser() != null) {
            presenceService.heartbeat(accessor.getUser().getName());
            log.info("[WS_CONNECT] userId={}", accessor.getUser().getName());
        }
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        if (accessor.getUser() != null) {
            presenceService.offline(accessor.getUser().getName());
            log.info("[WS_DISCONNECT] userId={}", accessor.getUser().getName());
        }
    }
}
