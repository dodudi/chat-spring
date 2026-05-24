package com.chat.websocket.handler;

import com.chat.message.application.MessageSender;
import com.chat.message.application.ReadCursorUpdater;
import com.chat.message.dto.MarkReadRequest;
import com.chat.message.dto.SendMessageRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class StompChatHandler {

    private final MessageSender messageSender;
    private final ReadCursorUpdater readCursorUpdater;

    @MessageMapping("/rooms/{roomId}/messages")
    public void sendMessage(
            @DestinationVariable UUID roomId,
            @Valid @Payload SendMessageRequest request,
            Principal principal) {
        messageSender.sendMessage(principal.getName(), roomId, request);
    }

    @MessageMapping("/rooms/{roomId}/read")
    public void markRead(
            @DestinationVariable UUID roomId,
            @Valid @Payload MarkReadRequest request,
            Principal principal) {
        readCursorUpdater.markRead(principal.getName(), roomId, request);
    }
}
