package com.chat.room.application;

import com.chat.common.exception.AppException;
import com.chat.common.exception.ErrorCode;
import com.chat.group.domain.UserGroup;
import com.chat.group.infrastructure.UserGroupRepository;
import com.chat.profile.domain.Profile;
import com.chat.profile.infrastructure.ProfileRepository;
import com.chat.room.domain.ChatRoom;
import com.chat.room.domain.ChatRoomMember;
import com.chat.room.domain.MemberRole;
import com.chat.room.domain.RoomGroupMembership;
import com.chat.room.dto.CreateDmRoomRequest;
import com.chat.room.dto.CreateGroupRoomRequest;
import com.chat.room.dto.CreatePublicRoomRequest;
import com.chat.room.dto.DmRoomResponse;
import com.chat.room.dto.PublicRoomResponse;
import com.chat.room.dto.RoomResponse;
import com.chat.room.infrastructure.ChatRoomMemberRepository;
import com.chat.room.infrastructure.ChatRoomRepository;
import com.chat.room.infrastructure.RoomGroupMembershipRepository;
import com.chat.user.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DefaultRoomManager implements RoomManager {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final RoomGroupMembershipRepository roomGroupMembershipRepository;
    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final UserGroupRepository userGroupRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public DmRoomResponse createDmRoom(String userId, CreateDmRoomRequest request) {
        if (!userRepository.existsById(request.targetUserId())) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }
        Profile creatorProfile = validateOwnProfile(userId, request.profileId());

        String roomKey = buildDmRoomKey(userId, request.targetUserId());
        return chatRoomRepository.findByRoomKey(roomKey)
                .map(existing -> {
                    chatRoomMemberRepository.findByRoomIdAndUserId(existing.getId(), userId)
                            .ifPresent(m -> m.updateProfileId(creatorProfile.getId()));
                    return DmRoomResponse.from(existing);
                })
                .orElseGet(() -> {
                    ChatRoom room = chatRoomRepository.save(ChatRoom.createDm(userId, roomKey));
                    chatRoomMemberRepository.save(ChatRoomMember.create(room.getId(), userId, creatorProfile.getId(), MemberRole.OWNER));

                    Profile targetProfile = profileRepository.findFirstByUserId(request.targetUserId())
                            .orElseThrow(() -> new AppException(ErrorCode.PROFILE_NOT_FOUND));
                    chatRoomMemberRepository.save(ChatRoomMember.create(room.getId(), request.targetUserId(), targetProfile.getId(), MemberRole.MEMBER));

                    addToDefaultGroup(userId, room.getId());
                    addToDefaultGroup(request.targetUserId(), room.getId());
                    return DmRoomResponse.from(room);
                });
    }

    @Override
    @Transactional
    public RoomResponse createGroupRoom(String userId, CreateGroupRoomRequest request) {
        Profile profile = validateOwnProfile(userId, request.profileId());
        ChatRoom room = chatRoomRepository.save(ChatRoom.createGroup(userId, request.name()));
        chatRoomMemberRepository.save(ChatRoomMember.create(room.getId(), userId, profile.getId(), MemberRole.OWNER));
        addToDefaultGroup(userId, room.getId());
        return RoomResponse.from(room);
    }

    @Override
    @Transactional
    public PublicRoomResponse createPublicRoom(String userId, CreatePublicRoomRequest request) {
        Profile profile = validateOwnProfile(userId, request.profileId());
        String hashedPassword = request.password() != null ? passwordEncoder.encode(request.password()) : null;
        ChatRoom room = chatRoomRepository.save(ChatRoom.createPublic(userId, request.name(), hashedPassword));
        chatRoomMemberRepository.save(ChatRoomMember.create(room.getId(), userId, profile.getId(), MemberRole.OWNER));
        addToDefaultGroup(userId, room.getId());
        return PublicRoomResponse.from(room);
    }

    private Profile validateOwnProfile(String userId, Long profileId) {
        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new AppException(ErrorCode.PROFILE_NOT_FOUND));
        if (!profile.getUserId().equals(userId)) {
            throw new AppException(ErrorCode.PROFILE_FORBIDDEN);
        }
        return profile;
    }

    private void addToDefaultGroup(String userId, UUID roomId) {
        UserGroup defaultGroup = userGroupRepository.findDefaultByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_NOT_FOUND));
        roomGroupMembershipRepository.save(RoomGroupMembership.create(roomId, defaultGroup.getId()));
    }

    private String buildDmRoomKey(String userId1, String userId2) {
        String[] users = {userId1, userId2};
        Arrays.sort(users);
        return "DM:" + users[0] + ":" + users[1];
    }
}
