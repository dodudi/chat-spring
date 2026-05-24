package com.chat.invitation.application;

import com.chat.common.exception.AppException;
import com.chat.common.exception.ErrorCode;
import com.chat.invitation.domain.Invitation;
import com.chat.invitation.dto.InvitationResponse;
import com.chat.invitation.infrastructure.InvitationRepository;
import com.chat.room.domain.ChatRoom;
import com.chat.room.domain.RoomType;
import com.chat.room.infrastructure.ChatRoomMemberRepository;
import com.chat.room.infrastructure.ChatRoomRepository;
import com.chat.user.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class DefaultInvitationSender implements InvitationSender {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final UserRepository userRepository;
    private final InvitationRepository invitationRepository;
    private final InvitationNotificationPublisher notificationPublisher;

    @Override
    public void sendInvitation(String inviterId, UUID roomId, String inviteeId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));

        if (room.getType() == RoomType.DM) {
            throw new AppException(ErrorCode.ROOM_TYPE_UNSUPPORTED);
        }

        if (!chatRoomMemberRepository.isOwner(roomId, inviterId)) {
            throw new AppException(ErrorCode.ROOM_FORBIDDEN);
        }

        if (!userRepository.existsById(inviteeId)) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }

        if (chatRoomMemberRepository.existsActiveMember(roomId, inviteeId)) {
            throw new AppException(ErrorCode.INVITATION_ALREADY_MEMBER);
        }

        if (invitationRepository.existsPending(roomId, inviteeId)) {
            throw new AppException(ErrorCode.INVITATION_DUPLICATE);
        }

        Invitation invitation = invitationRepository.save(Invitation.create(roomId, inviterId, inviteeId));
        InvitationResponse response = InvitationResponse.of(invitation, room);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                notificationPublisher.publish(inviteeId, response);
            }
        });
    }
}
