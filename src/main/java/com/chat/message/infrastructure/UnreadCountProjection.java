package com.chat.message.infrastructure;

import java.util.UUID;

public interface UnreadCountProjection {
    UUID getRoomId();
    Long getUnreadCount();
}
