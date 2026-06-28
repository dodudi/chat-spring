package kr.it.rudy.chat.server.dto;

public record CreateInviteRequest(
        Integer maxUses,
        Integer expiresInSeconds
) {
}
