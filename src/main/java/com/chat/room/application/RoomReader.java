package com.chat.room.application;

import com.chat.common.dto.PageResponse;
import com.chat.room.dto.PublicRoomSummaryResponse;
import com.chat.room.dto.RoomDetailResponse;
import com.chat.room.dto.RoomSummaryResponse;

import java.util.List;
import java.util.UUID;

public interface RoomReader {

    /** groupId 미지정 시 전체. DM 방 이름은 상대방 닉네임으로 동적 채움. */
    List<RoomSummaryResponse> getMyRooms(String userId, Long groupId);

    /** 예외: R001, C003. DM 방 이름은 상대방 닉네임으로 동적 채움. */
    RoomDetailResponse getRoomDetail(String userId, UUID roomId);

    /** 빈 방 제외, name 부분 일치, 페이지네이션. */
    PageResponse<PublicRoomSummaryResponse> searchPublicRooms(String name, int page, int size);
}
