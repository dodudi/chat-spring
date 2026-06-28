package kr.it.rudy.chat.channel.dto;

import kr.it.rudy.chat.channel.domain.ChannelCategory;

import java.time.Instant;

public record CategoryResponse(
        Long id,
        Long serverId,
        String name,
        int position,
        Instant createdAt
) {
    public static CategoryResponse from(ChannelCategory category) {
        return new CategoryResponse(
                category.getId(),
                category.getServer().getId(),
                category.getName(),
                category.getPosition(),
                category.getCreatedAt()
        );
    }
}
