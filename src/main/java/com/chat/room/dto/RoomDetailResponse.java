package com.chat.room.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RoomDetailResponse(
        UUID id,
        String type,
        String name,
        boolean hasPassword,
        long memberCount,
        OffsetDateTime createdAt
) {}
