package com.chat.room.infrastructure;

import java.util.UUID;

public interface RoomMemberCountProjection {
    UUID getRoomId();
    Long getMemberCount();
}
