package com.chat.message.dto;

import com.chat.message.domain.Message;
import com.chat.message.domain.MessageType;

import java.time.OffsetDateTime;

public record MessageResponse(
        Long id,
        String senderId,
        String content,
        MessageType type,
        OffsetDateTime createdAt
) {

    public static MessageResponse from(Message message) {
        return new MessageResponse(
                message.getId(),
                message.getSenderId(),
                message.getContent(),
                message.getType(),
                message.getCreatedAt()
        );
    }
}
