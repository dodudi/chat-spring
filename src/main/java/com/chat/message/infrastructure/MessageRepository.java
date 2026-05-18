package com.chat.message.infrastructure;

import com.chat.message.domain.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByRoomIdAndIdLessThanOrderByIdDesc(Long roomId, Long before, Pageable pageable);

    List<Message> findByRoomIdOrderByIdDesc(Long roomId, Pageable pageable);
}
