package kr.it.rudy.chat.message.dto;

import kr.it.rudy.chat.message.domain.Message;
import kr.it.rudy.chat.message.domain.MessageType;

import java.time.Instant;

public record MessageResponse(
        Long id,
        Long channelId,
        Long senderId,
        String senderExternalId,
        String content,
        MessageType type,
        Long parentMessageId,
        boolean isEdited,
        Instant deletedAt,
        Instant createdAt
) {
    public static MessageResponse from(Message message) {
        return new MessageResponse(
                message.getId(),
                message.getChannel().getId(),
                message.getSender().getId(),
                message.getSender().getExternalId(),
                message.getContent(),
                message.getType(),
                message.getParentMessage() != null ? message.getParentMessage().getId() : null,
                message.isEdited(),
                message.getDeletedAt(),
                message.getCreatedAt()
        );
    }
}
