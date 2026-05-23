package com.chat.room.dto;

import java.util.UUID;

public record PublicRoomSummaryResponse(
        UUID id,
        String name,
        long memberCount,
        boolean hasPassword
) {}
