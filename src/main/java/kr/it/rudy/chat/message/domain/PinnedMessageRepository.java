package kr.it.rudy.chat.message.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PinnedMessageRepository extends JpaRepository<PinnedMessage, Long> {
    boolean existsByChannelIdAndMessageId(Long channelId, Long messageId);
    Optional<PinnedMessage> findByChannelIdAndMessageId(Long channelId, Long messageId);
    List<PinnedMessage> findAllByChannelIdOrderByPinnedAtDesc(Long channelId);
}
