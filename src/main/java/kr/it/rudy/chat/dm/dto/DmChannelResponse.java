package kr.it.rudy.chat.dm.dto;

import kr.it.rudy.chat.dm.domain.DmChannel;
import kr.it.rudy.chat.dm.domain.DmChannelType;

import java.time.Instant;

public record DmChannelResponse(
        Long id,
        DmChannelType type,
        String name,
        String iconUrl,
        Instant createdAt
) {
    public static DmChannelResponse from(DmChannel channel) {
        return new DmChannelResponse(
                channel.getId(),
                channel.getType(),
                channel.getName(),
                channel.getIconUrl(),
                channel.getCreatedAt()
        );
    }
}
