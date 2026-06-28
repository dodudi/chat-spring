package kr.it.rudy.chat.dm.dto;

import kr.it.rudy.chat.dm.domain.DmMessage;

import java.time.Instant;

public record DmMessageResponse(
        Long id,
        Long dmChannelId,
        Long senderId,
        String senderExternalId,
        String content,
        Long parentMessageId,
        boolean isEdited,
        Instant createdAt
) {
    public static DmMessageResponse from(DmMessage message) {
        return new DmMessageResponse(
                message.getId(),
                message.getDmChannel().getId(),
                message.getSender().getId(),
                message.getSender().getExternalId(),
                message.getContent(),
                message.getParentMessage() != null ? message.getParentMessage().getId() : null,
                message.isEdited(),
                message.getCreatedAt()
        );
    }
}
