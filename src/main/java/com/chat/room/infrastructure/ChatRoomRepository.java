package com.chat.room.infrastructure;

import com.chat.room.domain.ChatRoom;
import com.chat.room.domain.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    Optional<ChatRoom> findByDmUserAAndDmUserBAndType(String dmUserA, String dmUserB, RoomType type);
}
