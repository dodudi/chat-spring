package com.chat.invitelink.dto;

import com.chat.invitelink.domain.InviteLink;

import java.time.OffsetDateTime;
import java.util.UUID;

public record InviteLinkResponse(
        Long id,
        UUID roomId,
        String token,
        OffsetDateTime expiresAt,
        boolean isActive,
        OffsetDateTime createdAt
) {
    public static InviteLinkResponse from(InviteLink link) {
        return new InviteLinkResponse(
                link.getId(),
                link.getRoomId(),
                link.getToken(),
                link.getExpiresAt(),
                link.isActive(),
                link.getCreatedAt());
    }
}
