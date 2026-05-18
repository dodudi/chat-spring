package com.chat.room.infrastructure;

import java.time.OffsetDateTime;

public interface RoomSummaryProjection {

    Long getId();
    String getType();
    String getName();
    String getDmUserA();
    String getDmUserB();
    OffsetDateTime getUpdatedAt();
    String getLastMessageContent();
    OffsetDateTime getLastMessageAt();
    long getUnreadCount();
}
