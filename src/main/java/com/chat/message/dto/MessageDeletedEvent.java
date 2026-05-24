package com.chat.message.dto;

import com.chat.common.RoomEvent;

import java.util.UUID;

public record MessageDeletedEvent(String type, UUID roomId, Long messageId) implements RoomEvent {

    public static MessageDeletedEvent of(UUID roomId, Long messageId) {
        return new MessageDeletedEvent("MESSAGE_DELETED", roomId, messageId);
    }
}
