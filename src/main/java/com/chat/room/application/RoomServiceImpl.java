package com.chat.room.application;

import com.chat.common.exception.AppException;
import com.chat.common.exception.ErrorCode;
import com.chat.room.domain.ChatRoom;
import com.chat.room.domain.ChatRoomMember;
import com.chat.room.domain.RoomType;
import com.chat.room.dto.CreateDmRoomRequest;
import com.chat.room.dto.CreateGroupRoomRequest;
import com.chat.room.dto.InviteMemberRequest;
import com.chat.room.dto.RoomSummaryResponse;
import com.chat.room.infrastructure.ChatRoomMemberRepository;
import com.chat.room.infrastructure.ChatRoomRepository;
import com.chat.websocket.dto.NotificationMessage;
import com.chat.websocket.redis.ChatMessagePublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomServiceImpl implements RoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatMessagePublisher chatMessagePublisher;

    @Override
    @Transactional
    public RoomSummaryResponse createOrGetDmRoom(String userId, CreateDmRoomRequest request) {
        String userA = userId.compareTo(request.targetUserId()) <= 0 ? userId : request.targetUserId();
        String userB = userId.compareTo(request.targetUserId()) <= 0 ? request.targetUserId() : userId;

        return chatRoomRepository.findByDmUserAAndDmUserBAndType(userA, userB, RoomType.DM)
                .map(existing -> chatRoomMemberRepository.findMyRoomsWithUnread(userId)
                        .stream()
                        .filter(p -> existing.getId().equals(p.getId()))
                        .findFirst()
                        .map(p -> RoomSummaryResponse.from(p, userId))
                        .orElseThrow(() -> new AppException(ErrorCode.ROOM_ACCESS_DENIED)))
                .orElseGet(() -> {
                    ChatRoom room = chatRoomRepository.save(ChatRoom.createDm(userId, request.targetUserId()));
                    chatRoomMemberRepository.save(ChatRoomMember.create(room, userId));
                    chatRoomMemberRepository.save(ChatRoomMember.create(room, request.targetUserId()));
                    return RoomSummaryResponse.fromNew(room, userId);
                });
    }

    @Override
    @Transactional
    public RoomSummaryResponse createGroupRoom(String userId, CreateGroupRoomRequest request) {
        ChatRoom room = chatRoomRepository.save(ChatRoom.createGroup(userId, request.name()));
        chatRoomMemberRepository.save(ChatRoomMember.create(room, userId));
        for (String memberId : request.memberIds()) {
            chatRoomMemberRepository.save(ChatRoomMember.create(room, memberId));
        }
        return RoomSummaryResponse.fromNew(room, userId);
    }

    @Override
    public List<RoomSummaryResponse> getMyRooms(String userId) {
        return chatRoomMemberRepository.findMyRoomsWithUnread(userId)
                .stream()
                .map(p -> RoomSummaryResponse.from(p, userId))
                .toList();
    }

    @Override
    @Transactional
    public void inviteMembers(String userId, Long roomId, InviteMemberRequest request) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
        if (room.getType() == RoomType.DM) {
            throw new AppException(ErrorCode.ROOM_ACCESS_DENIED);
        }
        NotificationMessage notification = new NotificationMessage(
                "ROOM_INVITED", room.getId(), room.getName(), userId);
        for (String invitedUserId : request.userIds()) {
            chatRoomMemberRepository.findMember(roomId, invitedUserId)
                    .ifPresentOrElse(
                            member -> { if (!member.isActive()) member.rejoin(); },
                            () -> chatRoomMemberRepository.save(ChatRoomMember.create(room, invitedUserId))
                    );
            chatMessagePublisher.publishNotification(invitedUserId, notification);
        }
    }

    @Override
    @Transactional
    public void leaveRoom(String userId, Long roomId) {
        ChatRoomMember member = chatRoomMemberRepository.findMember(roomId, userId)
                .filter(ChatRoomMember::isActive)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_ACCESS_DENIED));
        member.leave();
    }
}
