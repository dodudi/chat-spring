package com.chat.websocket.handler;

import com.chat.presence.application.PresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class StompPresenceHandler {

    private final PresenceService presenceService;

    @MessageMapping("/presence/heartbeat")
    public void heartbeat(Principal principal) {
        presenceService.heartbeat(principal.getName());
    }
}
