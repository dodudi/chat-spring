package kr.it.rudy.chat.channel.dto;

import kr.it.rudy.chat.channel.domain.Channel;
import kr.it.rudy.chat.channel.domain.ChannelType;

import java.time.Instant;

public record ChannelResponse(
        Long id,
        Long serverId,
        Long categoryId,
        ChannelType type,
        String name,
        String description,
        int position,
        boolean isNsfw,
        int slowmodeSeconds,
        Instant createdAt
) {
    public static ChannelResponse from(Channel channel) {
        return new ChannelResponse(
                channel.getId(),
                channel.getServer().getId(),
                channel.getCategory() != null ? channel.getCategory().getId() : null,
                channel.getType(),
                channel.getName(),
                channel.getDescription(),
                channel.getPosition(),
                channel.isNsfw(),
                channel.getSlowmodeSeconds(),
                channel.getCreatedAt()
        );
    }
}
