package com.chat.room.application;

import com.chat.room.dto.CreateDmRoomRequest;
import com.chat.room.dto.CreateGroupRoomRequest;
import com.chat.room.dto.CreatePublicRoomRequest;
import com.chat.room.dto.DmRoomResponse;
import com.chat.room.dto.PublicRoomResponse;
import com.chat.room.dto.RoomResponse;

public interface RoomCreator {

    /** DM 방 생성 (find-or-create). 동일 두 사용자 간 DM이 이미 존재하면 기존 방 반환. 예외: U001, P001, P002 */
    DmRoomResponse createDmRoom(String userId, CreateDmRoomRequest request);

    /** 예외: P001, P002 */
    RoomResponse createGroupRoom(String userId, CreateGroupRoomRequest request);

    /** 예외: P001, P002 */
    PublicRoomResponse createPublicRoom(String userId, CreatePublicRoomRequest request);
}
