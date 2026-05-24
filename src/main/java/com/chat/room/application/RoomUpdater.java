package com.chat.room.application;

import com.chat.room.dto.RoomResponse;
import com.chat.room.dto.UpdateRoomNameRequest;
import com.chat.room.dto.UpdateRoomPasswordRequest;

import java.util.UUID;

public interface RoomUpdater {

    /** GROUP·PUBLIC 공통. 예외: R001(없음), R010(DM 불가), R007(방장 아님) */
    RoomResponse updateName(String userId, UUID roomId, UpdateRoomNameRequest request);

    /** PUBLIC 전용. 예외: R001(없음), R010(GROUP·DM 불가), R007(방장 아님) */
    RoomResponse updatePassword(String userId, UUID roomId, UpdateRoomPasswordRequest request);

    /** PUBLIC 전용. 예외: R001(없음), R010(GROUP·DM 불가), R007(방장 아님) */
    RoomResponse clearPassword(String userId, UUID roomId);
}
