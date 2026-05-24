package com.chat.room.application;

import java.util.UUID;

public interface RoomKicker {

    /**
     * 예외: R001(없음), R010(DM 불가), R007(방장 아님), R011(자기 자신),
     *       R008(대상 멤버 없음·비활성)
     */
    void kickMember(String requesterId, UUID roomId, String targetUserId);
}
