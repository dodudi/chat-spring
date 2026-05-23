package com.chat.room.infrastructure;

import com.chat.room.domain.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, UUID> {

    @Query("SELECT r FROM ChatRoom r WHERE r.roomKey = :roomKey")
    Optional<ChatRoom> findByRoomKey(@Param("roomKey") String roomKey);
}
