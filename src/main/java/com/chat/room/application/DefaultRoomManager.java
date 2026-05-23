package com.chat.room.application;

import com.chat.common.dto.PageResponse;
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
import com.chat.room.domain.RoomType;
import com.chat.room.dto.CreateDmRoomRequest;
import com.chat.room.dto.CreateGroupRoomRequest;
import com.chat.room.dto.CreatePublicRoomRequest;
import com.chat.room.dto.DmRoomResponse;
import com.chat.room.dto.PublicRoomResponse;
import com.chat.room.dto.PublicRoomSummaryResponse;
import com.chat.room.dto.RoomDetailResponse;
import com.chat.room.dto.RoomResponse;
import com.chat.room.dto.RoomSummaryResponse;
import com.chat.room.infrastructure.ChatRoomMemberRepository;
import com.chat.room.infrastructure.ChatRoomRepository;
import com.chat.room.infrastructure.RoomMemberCountProjection;
import com.chat.room.infrastructure.RoomGroupMembershipRepository;
import com.chat.user.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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

    @Override
    public List<RoomSummaryResponse> getMyRooms(String userId, Long groupId) {
        return chatRoomRepository.findMyRooms(userId, groupId).stream()
                .map(room -> new RoomSummaryResponse(
                        room.getId(),
                        room.getType().name(),
                        resolveName(room, userId),
                        null, null, 0,
                        room.getUpdatedAt()))
                .toList();
    }

    @Override
    public RoomDetailResponse getRoomDetail(String userId, UUID roomId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
        if (room.getType() != RoomType.PUBLIC && !chatRoomMemberRepository.existsActiveMember(roomId, userId)) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
        long memberCount = chatRoomMemberRepository.countByRoomId(roomId);
        return new RoomDetailResponse(
                room.getId(),
                room.getType().name(),
                resolveName(room, userId),
                room.getPassword() != null,
                memberCount,
                room.getCreatedAt());
    }

    @Override
    public PageResponse<PublicRoomSummaryResponse> searchPublicRooms(String name, int page, int size) {
        Page<ChatRoom> result = chatRoomRepository.searchPublicRooms(name, PageRequest.of(page, size));
        List<UUID> roomIds = result.getContent().stream().map(ChatRoom::getId).toList();
        Map<UUID, Long> countMap = chatRoomMemberRepository.countByRoomIds(roomIds).stream()
                .collect(Collectors.toMap(RoomMemberCountProjection::getRoomId, RoomMemberCountProjection::getMemberCount));
        List<PublicRoomSummaryResponse> content = result.getContent().stream()
                .map(room -> new PublicRoomSummaryResponse(
                        room.getId(),
                        room.getName(),
                        countMap.getOrDefault(room.getId(), 0L),
                        room.getPassword() != null))
                .toList();
        return new PageResponse<>(content, page, size, result.getTotalElements());
    }

    private String resolveName(ChatRoom room, String userId) {
        if (room.getType() != RoomType.DM) {
            return room.getName();
        }
        return chatRoomMemberRepository.findOtherMember(room.getId(), userId)
                .flatMap(other -> profileRepository.findById(other.getProfileId()))
                .map(Profile::getNickname)
                .orElse("알 수 없음");
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
