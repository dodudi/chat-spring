package com.chat.websocket.handler;

import com.chat.message.application.MessageService;
import com.chat.message.domain.Message;
import com.chat.websocket.dto.ChatBroadcastMessage;
import com.chat.websocket.dto.MarkReadRequest;
import com.chat.websocket.dto.SendMessageRequest;
import com.chat.websocket.redis.ChatMessagePublisher;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
@Slf4j
public class StompChatHandler {

    private final MessageService messageService;
    private final ChatMessagePublisher chatMessagePublisher;

    @MessageMapping("/rooms/{roomId}/messages")
    public void sendMessage(@DestinationVariable Long roomId,
                            @Valid @Payload SendMessageRequest request,
                            Principal principal) {
        String userId = principal.getName();
        log.info("[WS_SEND] userId={} roomId={}", userId, roomId);
        Message saved = messageService.sendMessage(userId, roomId, request.content());
        chatMessagePublisher.publish(roomId, ChatBroadcastMessage.from(saved));
    }

    @MessageMapping("/rooms/{roomId}/read")
    public void markRead(@DestinationVariable Long roomId,
                         @Valid @Payload MarkReadRequest request,
                         Principal principal) {
        String userId = principal.getName();
        messageService.markRead(userId, roomId, request.messageId());
    }
}
