package com.chat.room.application;

import java.util.UUID;

public interface RoomMemberUpdater {

    void updateProfile(String userId, UUID roomId, Long profileId);
}
