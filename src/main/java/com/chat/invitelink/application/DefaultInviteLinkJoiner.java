package com.chat.invitelink.application;

import com.chat.common.exception.AppException;
import com.chat.common.exception.ErrorCode;
import com.chat.group.domain.UserGroup;
import com.chat.group.infrastructure.UserGroupRepository;
import com.chat.invitelink.domain.InviteLink;
import com.chat.invitelink.dto.JoinByLinkRequest;
import com.chat.invitelink.infrastructure.InviteLinkRepository;
import com.chat.profile.application.ProfileValidator;
import com.chat.profile.domain.Profile;
import com.chat.room.application.RoomCapacityPolicy;
import com.chat.room.domain.ChatRoom;
import com.chat.room.domain.ChatRoomMember;
import com.chat.room.domain.MemberRole;
import com.chat.room.domain.RoomGroupMembership;
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
public class DefaultInviteLinkJoiner implements InviteLinkJoiner {

    private final InviteLinkRepository inviteLinkRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ProfileValidator profileValidator;
    private final RoomCapacityPolicy roomCapacityPolicy;
    private final UserGroupRepository userGroupRepository;
    private final RoomGroupMembershipRepository roomGroupMembershipRepository;

    @Override
    public void joinByLink(String userId, String token, JoinByLinkRequest request) {
        InviteLink link = inviteLinkRepository.findByToken(token)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));

        if (!link.isActive()) {
            throw new AppException(ErrorCode.INVITE_LINK_INACTIVE);
        }
        if (link.isExpired()) {
            throw new AppException(ErrorCode.INVITE_LINK_EXPIRED);
        }

        ChatRoom room = chatRoomRepository.findById(link.getRoomId())
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));

        long activeCount = chatRoomMemberRepository.countActiveByRoomId(room.getId());
        if (activeCount >= roomCapacityPolicy.maxCapacity(room.getType())) {
            throw new AppException(ErrorCode.ROOM_MEMBER_LIMIT);
        }

        // 초대 링크는 강퇴 제한 해제 — 기존 멤버 레코드 삭제 후 재입장 허용
        chatRoomMemberRepository.findByRoomIdAndUserId(room.getId(), userId).ifPresent(m -> {
            if (m.getLeftAt() == null && m.getKickedAt() == null) {
                throw new AppException(ErrorCode.ROOM_ALREADY_JOINED);
            }
            chatRoomMemberRepository.delete(m);
        });

        Profile profile = profileValidator.validateOwnership(userId, request.profileId());
        chatRoomMemberRepository.save(ChatRoomMember.create(room.getId(), userId, profile.getId(), MemberRole.MEMBER));
        addToDefaultGroup(userId, room.getId());
    }

    private void addToDefaultGroup(String userId, UUID roomId) {
        UserGroup defaultGroup = userGroupRepository.findDefaultByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_NOT_FOUND));
        if (roomGroupMembershipRepository.findByRoomIdAndGroupId(roomId, defaultGroup.getId()).isEmpty()) {
            roomGroupMembershipRepository.save(RoomGroupMembership.create(roomId, defaultGroup.getId()));
        }
    }
}
