package com.chat.websocket.dto;

import com.chat.message.domain.Message;
import com.chat.message.domain.MessageType;

import java.time.OffsetDateTime;

public record ChatBroadcastMessage(
        Long id,
        Long roomId,
        String senderId,
        String content,
        MessageType type,
        OffsetDateTime createdAt
) {

    public static ChatBroadcastMessage from(Message message) {
        return new ChatBroadcastMessage(
                message.getId(),
                message.getRoomId(),
                message.getSenderId(),
                message.getContent(),
                message.getType(),
                message.getCreatedAt()
        );
    }
}
