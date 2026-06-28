package kr.it.rudy.chat.message.domain;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByChannelIdAndDeletedAtIsNullOrderByIdDesc(Long channelId, Pageable pageable);
    List<Message> findByChannelIdAndDeletedAtIsNullAndIdLessThanOrderByIdDesc(Long channelId, Long before, Pageable pageable);
}
