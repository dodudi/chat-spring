package com.chat.room.application;

import com.chat.room.dto.JoinPublicRoomRequest;
import com.chat.room.dto.RoomResponse;

import java.util.UUID;

public interface RoomJoiner {

    /**
     * 예외: R001(없음), R010(타입 불일치), R009(빈 방), R003(이미 참여),
     *       R006(강퇴됨), R004(인원 초과), P001(프로필 없음), P002(본인 아님), R005(비밀번호 불일치)
     */
    RoomResponse joinPublicRoom(String userId, UUID roomId, JoinPublicRoomRequest request);
}
