package com.chat.group.application;

import java.util.UUID;

public interface RoomGroupManager {

    /** 예외: G001(그룹 없음), C003(방 멤버 아님), R003(이미 할당됨) */
    void assignRoom(String userId, Long groupId, UUID roomId);

    /** 예외: G001(그룹 없음), G004(기본 그룹) */
    void removeRoom(String userId, Long groupId, UUID roomId);
}
