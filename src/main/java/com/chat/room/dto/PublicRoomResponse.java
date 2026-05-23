package com.chat.room.dto;

import com.chat.room.domain.ChatRoom;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PublicRoomResponse(
        UUID id,
        String type,
        String roomKey,
        String name,
        boolean hasPassword,
        OffsetDateTime createdAt
) {
    public static PublicRoomResponse from(ChatRoom room) {
        return new PublicRoomResponse(
                room.getId(), room.getType().name(), room.getRoomKey(),
                room.getName(), room.getPassword() != null, room.getCreatedAt());
    }
}
