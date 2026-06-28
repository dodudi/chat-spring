package kr.it.rudy.chat.server.dto;

import kr.it.rudy.chat.server.domain.ServerInvite;

import java.time.Instant;

public record InviteResponse(
        Long id,
        Long serverId,
        String code,
        Integer maxUses,
        int uses,
        Instant expiresAt,
        Instant createdAt
) {
    public static InviteResponse from(ServerInvite invite) {
        return new InviteResponse(
                invite.getId(),
                invite.getServer().getId(),
                invite.getCode(),
                invite.getMaxUses(),
                invite.getUses(),
                invite.getExpiresAt(),
                invite.getCreatedAt()
        );
    }
}
