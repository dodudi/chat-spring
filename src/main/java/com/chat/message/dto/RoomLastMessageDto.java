package com.chat.message.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RoomLastMessageDto(
        UUID roomId,
        String content,
        OffsetDateTime createdAt
) {}
