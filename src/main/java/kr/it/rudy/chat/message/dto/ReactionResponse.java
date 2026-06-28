package kr.it.rudy.chat.message.dto;

import kr.it.rudy.chat.message.domain.MessageReaction;

import java.time.Instant;

public record ReactionResponse(
        Long id,
        Long messageId,
        Long userId,
        String emoji,
        Instant createdAt
) {
    public static ReactionResponse from(MessageReaction reaction) {
        return new ReactionResponse(
                reaction.getId(),
                reaction.getMessage().getId(),
                reaction.getUser().getId(),
                reaction.getEmoji(),
                reaction.getCreatedAt()
        );
    }
}
