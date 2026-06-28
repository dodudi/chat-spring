package kr.it.rudy.chat.server.dto;

import kr.it.rudy.chat.server.domain.Server;

import java.time.Instant;

public record ServerResponse(
        Long id,
        Long ownerId,
        String name,
        String description,
        String iconUrl,
        String inviteCode,
        boolean isPublic,
        Instant createdAt
) {
    public static ServerResponse from(Server server) {
        return new ServerResponse(
                server.getId(),
                server.getOwner().getId(),
                server.getName(),
                server.getDescription(),
                server.getIconUrl(),
                server.getInviteCode(),
                server.isPublic(),
                server.getCreatedAt()
        );
    }
}
