package com.chat.message.infrastructure;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface LastMessageProjection {
    UUID getRoomId();
    String getContent();
    OffsetDateTime getCreatedAt();
}
