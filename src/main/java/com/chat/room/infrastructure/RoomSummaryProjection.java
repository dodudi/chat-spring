package com.chat.room.infrastructure;

import java.time.Instant;

public interface RoomSummaryProjection {

    Long getId();
    String getType();
    String getName();
    String getDmUserA();
    String getDmUserB();
    Instant getUpdatedAt();
    String getLastMessageContent();
    Instant getLastMessageAt();
    long getUnreadCount();
}
