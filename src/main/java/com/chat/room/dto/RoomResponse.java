package com.chat.room.dto;

import com.chat.room.domain.ChatRoom;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RoomResponse(
        UUID id,
        String type,
        String roomKey,
        String name,
        OffsetDateTime createdAt
) {
    public static RoomResponse from(ChatRoom room) {
        return new RoomResponse(room.getId(), room.getType().name(), room.getRoomKey(), room.getName(), room.getCreatedAt());
    }
}
