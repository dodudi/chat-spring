package com.chat.group.application;

import com.chat.common.exception.AppException;
import com.chat.common.exception.ErrorCode;
import com.chat.group.domain.UserGroup;
import com.chat.group.infrastructure.UserGroupRepository;
import com.chat.room.domain.RoomGroupMembership;
import com.chat.room.infrastructure.ChatRoomMemberRepository;
import com.chat.room.infrastructure.RoomGroupMembershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class DefaultRoomGroupManager implements RoomGroupManager {

    private final UserGroupRepository userGroupRepository;
    private final RoomGroupMembershipRepository roomGroupMembershipRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;

    @Override
    public void assignRoom(String userId, Long groupId, UUID roomId) {
        UserGroup group = findOwnGroup(userId, groupId);
        if (!chatRoomMemberRepository.existsActiveMember(roomId, userId)) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
        if (roomGroupMembershipRepository.findByRoomIdAndGroupId(roomId, group.getId()).isPresent()) {
            throw new AppException(ErrorCode.GROUP_ROOM_ALREADY_ASSIGNED);
        }
        roomGroupMembershipRepository.save(RoomGroupMembership.create(roomId, group.getId()));
    }

    @Override
    public void removeRoom(String userId, Long groupId, UUID roomId) {
        UserGroup group = findOwnGroup(userId, groupId);
        if (group.isDefault()) {
            throw new AppException(ErrorCode.GROUP_DEFAULT_IMMUTABLE);
        }
        roomGroupMembershipRepository.deleteByRoomIdAndGroupId(roomId, group.getId());
    }

    private UserGroup findOwnGroup(String userId, Long groupId) {
        return userGroupRepository.findByIdAndUserId(groupId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_NOT_FOUND));
    }
}
