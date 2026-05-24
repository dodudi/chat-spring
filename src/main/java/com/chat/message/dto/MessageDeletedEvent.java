package com.chat.message.dto;

import java.util.UUID;

public record MessageDeletedEvent(String type, UUID roomId, Long messageId) {

    public static MessageDeletedEvent of(UUID roomId, Long messageId) {
        return new MessageDeletedEvent("MESSAGE_DELETED", roomId, messageId);
    }
}
