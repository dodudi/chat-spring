package com.chat.room.infrastructure;

import java.util.UUID;

public interface DmRoomNameProjection {
    UUID getRoomId();
    String getNickname();
}
