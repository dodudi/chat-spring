package com.chat.message.dto;

import com.chat.message.domain.Message;

import java.time.OffsetDateTime;

public record MessageResponse(
        Long id,
        String senderId,
        String senderNickname,
        String content,
        String type,
        boolean edited,
        OffsetDateTime createdAt
) {
    public static MessageResponse of(Message message, String senderNickname) {
        return new MessageResponse(
                message.getId(),
                message.getSenderId(),
                senderNickname,
                message.getContent(),
                message.getType().name(),
                message.isEdited(),
                message.getCreatedAt());
    }
}
