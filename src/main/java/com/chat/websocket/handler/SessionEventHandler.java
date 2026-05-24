package com.chat.websocket.handler;

import com.chat.invitation.application.InvitationReader;
import com.chat.invitation.dto.InvitationResponse;
import com.chat.presence.application.PresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionEventHandler {

    private final PresenceService presenceService;
    private final InvitationReader invitationReader;
    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handleConnect(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        if (accessor.getUser() == null) return;

        String userId = accessor.getUser().getName();
        presenceService.heartbeat(userId);
        log.info("[WS_CONNECT] userId={}", userId);

        List<InvitationResponse> pending = invitationReader.getPendingInvitations(userId);
        pending.forEach(inv ->
                messagingTemplate.convertAndSendToUser(userId, "/queue/notifications", inv));
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
