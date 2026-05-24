package com.chat.room.application;

import java.util.UUID;

public interface RoomLeaver {

    /**
     * DM: is_hidden = true (멱등)
     * GROUP: left_at 기록, 방장이면 위임, 그룹 연결 해제
     * PUBLIC: 레코드 삭제, 방장이면 위임, 그룹 연결 해제
     *
     * 예외: R001(방 없음), R008(비활성 멤버)
     */
    void leaveRoom(String userId, UUID roomId);
}
