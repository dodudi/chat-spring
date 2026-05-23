package com.chat.room.application;

import com.chat.common.dto.PageResponse;
import com.chat.room.dto.CreateDmRoomRequest;
import com.chat.room.dto.CreateGroupRoomRequest;
import com.chat.room.dto.CreatePublicRoomRequest;
import com.chat.room.dto.DmRoomResponse;
import com.chat.room.dto.PublicRoomResponse;
import com.chat.room.dto.PublicRoomSummaryResponse;
import com.chat.room.dto.RoomDetailResponse;
import com.chat.room.dto.RoomResponse;
import com.chat.room.dto.RoomSummaryResponse;

import java.util.List;
import java.util.UUID;

public interface RoomManager {

    /**
     * DM 방 생성 (find-or-create).
     * 동일 두 사용자 간 DM이 이미 존재하면 기존 방 반환. 항상 201.
     * 기존 방 반환 시에도 요청한 profileId로 chat_room_members.profile_id 갱신.
     * 예외: U001, P001, P002
     */
    DmRoomResponse createDmRoom(String userId, CreateDmRoomRequest request);

    /** 예외: P001, P002 */
    RoomResponse createGroupRoom(String userId, CreateGroupRoomRequest request);

    /** 예외: P001, P002 */
    PublicRoomResponse createPublicRoom(String userId, CreatePublicRoomRequest request);

    /** groupId 미지정 시 전체. DM 방 이름은 상대방 닉네임으로 동적 채움. */
    List<RoomSummaryResponse> getMyRooms(String userId, Long groupId);

    /** 예외: R001. DM 방 이름은 상대방 닉네임으로 동적 채움. */
    RoomDetailResponse getRoomDetail(String userId, UUID roomId);

    /** 빈 방 제외, name 부분 일치, 페이지네이션. */
    PageResponse<PublicRoomSummaryResponse> searchPublicRooms(String name, int page, int size);
}
