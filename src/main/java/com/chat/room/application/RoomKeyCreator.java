package com.chat.room.application;

public interface RoomKeyCreator {

    String createDmRoomKey(String userId1, String userId2);

    String createGroupRoomKey();

    String createPublicRoomKey();

}
