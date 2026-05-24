package com.chat.room.application;

import com.chat.common.exception.AppException;
import com.chat.common.exception.ErrorCode;
import com.chat.room.domain.ChatRoom;
import com.chat.room.domain.ChatRoomMember;
import com.chat.room.domain.MemberRole;
import com.chat.room.domain.RoomType;
import com.chat.room.infrastructure.ChatRoomMemberRepository;
import com.chat.room.infrastructure.ChatRoomRepository;
import com.chat.room.infrastructure.RoomGroupMembershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class DefaultRoomLeaver implements RoomLeaver {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final RoomGroupMembershipRepository roomGroupMembershipRepository;

    @Override
    public void leaveRoom(String userId, UUID roomId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));

        switch (room.getType()) {
            case DM -> leaveDm(userId, roomId);
            case GROUP -> leaveGroup(userId, roomId);
            case PUBLIC -> leavePublic(userId, roomId);
        }
    }

    private void leaveDm(String userId, UUID roomId) {
        ChatRoomMember member = chatRoomMemberRepository.findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_MEMBER_NOT_FOUND));
        if (member.isHidden()) return;
        member.hide();
    }

    private void leaveGroup(String userId, UUID roomId) {
        ChatRoomMember member = resolveActiveMember(userId, roomId);
        prepareLeave(member, roomId, userId);
        member.leave();
    }

    private void leavePublic(String userId, UUID roomId) {
        ChatRoomMember member = resolveActiveMember(userId, roomId);
        prepareLeave(member, roomId, userId);
        chatRoomMemberRepository.delete(member);
    }

    private ChatRoomMember resolveActiveMember(String userId, UUID roomId) {
        return chatRoomMemberRepository.findByRoomIdAndUserId(roomId, userId)
                .filter(m -> m.getLeftAt() == null && m.getKickedAt() == null)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_MEMBER_NOT_FOUND));
    }

    private void prepareLeave(ChatRoomMember member, UUID roomId, String userId) {
        if (member.getRole() == MemberRole.OWNER) {
            delegateOwner(roomId, userId);
        }
        roomGroupMembershipRepository.deleteByRoomIdAndUserId(roomId, userId);
    }

    private void delegateOwner(UUID roomId, String leavingUserId) {
        List<ChatRoomMember> candidates = chatRoomMemberRepository
                .findActiveMembersExcluding(roomId, leavingUserId, PageRequest.of(0, 1));
        if (!candidates.isEmpty()) {
            candidates.get(0).promoteToOwner();
        }
    }
}
