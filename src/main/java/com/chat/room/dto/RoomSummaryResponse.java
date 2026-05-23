package com.chat.room.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RoomSummaryResponse(
        UUID id,
        String type,
        String name,
        String lastMessage,
        OffsetDateTime lastMessageAt,
        int unreadCount,
        OffsetDateTime updatedAt
) {}
