package com.chat.room.application;

import com.chat.room.dto.CreateDmRoomRequest;
import com.chat.room.dto.CreateGroupRoomRequest;
import com.chat.room.dto.InviteMemberRequest;
import com.chat.room.dto.RoomSummaryResponse;

import java.util.List;

public interface RoomService {

    RoomSummaryResponse createOrGetDmRoom(String userId, CreateDmRoomRequest request);

    RoomSummaryResponse createGroupRoom(String userId, CreateGroupRoomRequest request);

    List<RoomSummaryResponse> getMyRooms(String userId);

    void inviteMembers(String userId, Long roomId, InviteMemberRequest request);

    void leaveRoom(String userId, Long roomId);
}
