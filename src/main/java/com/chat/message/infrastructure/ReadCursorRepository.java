package com.chat.message.infrastructure;

import com.chat.message.domain.ReadCursor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ReadCursorRepository extends JpaRepository<ReadCursor, Long> {

    @Query("SELECT c FROM ReadCursor c WHERE c.roomId = :roomId AND c.userId = :userId")
    Optional<ReadCursor> findByRoomIdAndUserId(@Param("roomId") UUID roomId, @Param("userId") String userId);
}
