package com.chat.room.dto;

import com.chat.room.domain.ChatRoom;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DmRoomResponse(
        UUID id,
        String type,
        String roomKey,
        OffsetDateTime createdAt
) {
    public static DmRoomResponse from(ChatRoom room) {
        return new DmRoomResponse(room.getId(), room.getType().name(), room.getRoomKey(), room.getCreatedAt());
    }
}
