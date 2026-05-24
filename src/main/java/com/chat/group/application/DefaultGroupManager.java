package com.chat.group.application;

import com.chat.common.exception.AppException;
import com.chat.common.exception.ErrorCode;
import com.chat.group.domain.UserGroup;
import com.chat.group.dto.CreateGroupRequest;
import com.chat.group.dto.GroupResponse;
import com.chat.group.dto.UpdateGroupRequest;
import com.chat.group.infrastructure.UserGroupRepository;
import com.chat.room.infrastructure.RoomGroupMembershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class DefaultGroupManager implements GroupManager {

    private final UserGroupRepository userGroupRepository;
    private final RoomGroupMembershipRepository roomGroupMembershipRepository;
    private final GroupPolicy groupPolicy;

    @Override
    public GroupResponse createGroup(String userId, CreateGroupRequest request) {
        if (userGroupRepository.existsByUserIdAndName(userId, request.name())) {
            throw new AppException(ErrorCode.GROUP_NAME_DUPLICATE);
        }
        if (userGroupRepository.countCustomByUserId(userId) >= groupPolicy.maxGroupCount()) {
            throw new AppException(ErrorCode.GROUP_LIMIT_EXCEEDED);
        }
        UserGroup group = userGroupRepository.save(UserGroup.create(userId, request.name()));
        return GroupResponse.from(group);
    }

    @Override
    public GroupResponse renameGroup(String userId, Long groupId, UpdateGroupRequest request) {
        UserGroup group = findOwnGroup(userId, groupId);
        if (group.isDefault()) {
            throw new AppException(ErrorCode.GROUP_DEFAULT_IMMUTABLE);
        }
        if (userGroupRepository.existsByUserIdAndNameExcluding(userId, request.name(), groupId)) {
            throw new AppException(ErrorCode.GROUP_NAME_DUPLICATE);
        }
        group.rename(request.name());
        return GroupResponse.from(group);
    }

    @Override
    public void deleteGroup(String userId, Long groupId) {
        UserGroup group = findOwnGroup(userId, groupId);
        if (group.isDefault()) {
            throw new AppException(ErrorCode.GROUP_DEFAULT_IMMUTABLE);
        }
        roomGroupMembershipRepository.deleteByGroupId(group.getId());
        userGroupRepository.delete(group);
    }

    private UserGroup findOwnGroup(String userId, Long groupId) {
        return userGroupRepository.findByIdAndUserId(groupId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_NOT_FOUND));
    }
}
