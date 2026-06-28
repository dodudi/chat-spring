package kr.it.rudy.chat.server.dto;

import kr.it.rudy.chat.server.domain.ServerMember;

import java.time.Instant;

public record ServerMemberResponse(
        Long id,
        Long userId,
        String externalId,
        String nickname,
        Instant joinedAt
) {
    public static ServerMemberResponse from(ServerMember member) {
        return new ServerMemberResponse(
                member.getId(),
                member.getUser().getId(),
                member.getUser().getExternalId(),
                member.getNickname(),
                member.getJoinedAt()
        );
    }
}
