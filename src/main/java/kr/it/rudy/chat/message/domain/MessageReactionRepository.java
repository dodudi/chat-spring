package kr.it.rudy.chat.message.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MessageReactionRepository extends JpaRepository<MessageReaction, Long> {
    boolean existsByMessageIdAndUserIdAndEmoji(Long messageId, Long userId, String emoji);
    Optional<MessageReaction> findByMessageIdAndUserIdAndEmoji(Long messageId, Long userId, String emoji);
}
