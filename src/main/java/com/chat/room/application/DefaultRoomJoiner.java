package com.chat.room.application;

import com.chat.common.exception.AppException;
import com.chat.common.exception.ErrorCode;
import com.chat.group.domain.UserGroup;
import com.chat.group.infrastructure.UserGroupRepository;
import com.chat.profile.application.ProfileValidator;
import com.chat.profile.domain.Profile;
import com.chat.room.domain.ChatRoom;
import com.chat.room.domain.ChatRoomMember;
import com.chat.room.domain.MemberRole;
import com.chat.room.domain.RoomGroupMembership;
import com.chat.room.domain.RoomType;
import com.chat.room.dto.JoinPublicRoomRequest;
import com.chat.room.dto.RoomResponse;
import com.chat.room.infrastructure.ChatRoomMemberRepository;
import com.chat.room.infrastructure.ChatRoomRepository;
import com.chat.room.infrastructure.RoomGroupMembershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class DefaultRoomJoiner implements RoomJoiner {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ProfileValidator profileValidator;
    private final RoomPasswordEncoder roomPasswordEncoder;
    private final RoomCapacityPolicy roomCapacityPolicy;
    private final UserGroupRepository userGroupRepository;
    private final RoomGroupMembershipRepository roomGroupMembershipRepository;

    @Override
    public RoomResponse joinPublicRoom(String userId, UUID roomId, JoinPublicRoomRequest request) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
        if (room.getType() != RoomType.PUBLIC) {
            throw new AppException(ErrorCode.ROOM_TYPE_UNSUPPORTED);
        }

        long activeCount = chatRoomMemberRepository.countActiveByRoomId(roomId);
        if (activeCount == 0) {
            throw new AppException(ErrorCode.ROOM_EMPTY);
        }

        chatRoomMemberRepository.findByRoomIdAndUserId(roomId, userId).ifPresent(m -> {
            if (m.getKickedAt() != null) throw new AppException(ErrorCode.ROOM_KICKED);
            if (m.getLeftAt() == null)   throw new AppException(ErrorCode.ROOM_ALREADY_JOINED);
            // leftAt != null: 이전 퇴장 레코드 삭제 후 재입장 허용 (UNIQUE 제약 방지)
            chatRoomMemberRepository.delete(m);
        });

        if (activeCount >= roomCapacityPolicy.maxCapacity(RoomType.PUBLIC)) {
            throw new AppException(ErrorCode.ROOM_MEMBER_LIMIT);
        }

        Profile profile = profileValidator.validateOwnership(userId, request.profileId());

        if (room.getPassword() != null && !roomPasswordEncoder.matches(request.password(), room.getPassword())) {
            throw new AppException(ErrorCode.ROOM_PASSWORD_MISMATCH);
        }

        chatRoomMemberRepository.save(ChatRoomMember.create(roomId, userId, profile.getId(), MemberRole.MEMBER));
        addToDefaultGroup(userId, roomId);

        return RoomResponse.from(room);
    }

    private void addToDefaultGroup(String userId, UUID roomId) {
        UserGroup defaultGroup = userGroupRepository.findDefaultByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_NOT_FOUND));
        if (roomGroupMembershipRepository.findByRoomIdAndGroupId(roomId, defaultGroup.getId()).isEmpty()) {
            roomGroupMembershipRepository.save(RoomGroupMembership.create(roomId, defaultGroup.getId()));
        }
    }
}
