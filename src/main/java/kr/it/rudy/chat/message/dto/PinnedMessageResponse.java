package kr.it.rudy.chat.message.dto;

import kr.it.rudy.chat.message.domain.PinnedMessage;

import java.time.Instant;

public record PinnedMessageResponse(
        Long id,
        Long channelId,
        Long messageId,
        Long pinnedById,
        Instant pinnedAt
) {
    public static PinnedMessageResponse from(PinnedMessage pin) {
        return new PinnedMessageResponse(
                pin.getId(),
                pin.getChannel().getId(),
                pin.getMessage().getId(),
                pin.getPinnedBy().getId(),
                pin.getPinnedAt()
        );
    }
}
