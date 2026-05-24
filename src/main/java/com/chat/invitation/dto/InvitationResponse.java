package com.chat.invitation.dto;

import com.chat.invitation.domain.Invitation;
import com.chat.invitation.domain.InvitationStatus;
import com.chat.room.domain.ChatRoom;

import java.time.OffsetDateTime;
import java.util.UUID;

public record InvitationResponse(
        Long id,
        UUID roomId,
        String roomName,
        String inviterId,
        InvitationStatus status,
        OffsetDateTime createdAt
) {
    public static InvitationResponse of(Invitation invitation, ChatRoom room) {
        return new InvitationResponse(
                invitation.getId(),
                invitation.getRoomId(),
                room.getName(),
                invitation.getInviterId(),
                invitation.getStatus(),
                invitation.getCreatedAt()
        );
    }
}
