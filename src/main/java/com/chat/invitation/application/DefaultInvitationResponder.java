package com.chat.invitation.application;

import com.chat.common.exception.AppException;
import com.chat.common.exception.ErrorCode;
import com.chat.group.domain.UserGroup;
import com.chat.group.infrastructure.UserGroupRepository;
import com.chat.invitation.domain.Invitation;
import com.chat.invitation.domain.InvitationStatus;
import com.chat.invitation.dto.AcceptInvitationRequest;
import com.chat.invitation.infrastructure.InvitationRepository;
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
public class DefaultInvitationResponder implements InvitationResponder {

    private final InvitationRepository invitationRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ProfileValidator profileValidator;
    private final RoomCapacityPolicy roomCapacityPolicy;
    private final UserGroupRepository userGroupRepository;
    private final RoomGroupMembershipRepository roomGroupMembershipRepository;

    @Override
    public void accept(String userId, Long invitationId, AcceptInvitationRequest request) {
        Invitation invitation = findPendingInvitation(userId, invitationId);

        ChatRoom room = chatRoomRepository.findById(invitation.getRoomId())
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));

        long activeCount = chatRoomMemberRepository.countActiveByRoomId(room.getId());
        if (activeCount >= roomCapacityPolicy.maxCapacity(room.getType())) {
            throw new AppException(ErrorCode.ROOM_MEMBER_LIMIT);
        }

        chatRoomMemberRepository.findByRoomIdAndUserId(room.getId(), userId).ifPresent(m -> {
            if (m.getKickedAt() != null) throw new AppException(ErrorCode.ROOM_KICKED);
            if (m.getLeftAt() == null)   throw new AppException(ErrorCode.INVITATION_ALREADY_MEMBER);
            // leftAt != null: 이전 퇴장 레코드 삭제 후 재입장
            chatRoomMemberRepository.delete(m);
        });

        Profile profile = profileValidator.validateOwnership(userId, request.profileId());
        chatRoomMemberRepository.save(ChatRoomMember.create(room.getId(), userId, profile.getId(), MemberRole.MEMBER));
        addToDefaultGroup(userId, room.getId());

        invitation.accept();
    }

    @Override
    public void reject(String userId, Long invitationId) {
        Invitation invitation = findPendingInvitation(userId, invitationId);
        invitation.reject();
    }

    private Invitation findPendingInvitation(String userId, Long invitationId) {
        Invitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new AppException(ErrorCode.INVITATION_NOT_FOUND));
        if (!invitation.getInviteeId().equals(userId)) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new AppException(ErrorCode.INVITATION_ALREADY_PROCESSED);
        }
        return invitation;
    }

    private void addToDefaultGroup(String userId, UUID roomId) {
        UserGroup defaultGroup = userGroupRepository.findDefaultByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_NOT_FOUND));
        if (roomGroupMembershipRepository.findByRoomIdAndGroupId(roomId, defaultGroup.getId()).isEmpty()) {
            roomGroupMembershipRepository.save(RoomGroupMembership.create(roomId, defaultGroup.getId()));
        }
    }
}
