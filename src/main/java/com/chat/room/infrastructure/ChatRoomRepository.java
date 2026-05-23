package com.chat.room.infrastructure;

import com.chat.room.domain.ChatRoom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, UUID> {

    @Query("SELECT r FROM ChatRoom r WHERE r.roomKey = :roomKey")
    Optional<ChatRoom> findByRoomKey(@Param("roomKey") String roomKey);

    @Query("""
            SELECT r FROM ChatRoom r
            WHERE EXISTS (
                SELECT 1 FROM ChatRoomMember m
                WHERE m.roomId = r.id AND m.userId = :userId AND m.leftAt IS NULL AND m.kickedAt IS NULL AND m.isHidden = false
            )
            AND (:groupId IS NULL OR EXISTS (
                SELECT 1 FROM RoomGroupMembership g
                WHERE g.roomId = r.id AND g.groupId = :groupId
            ))
            ORDER BY r.updatedAt DESC
            """)
    List<ChatRoom> findMyRooms(@Param("userId") String userId, @Param("groupId") Long groupId);

    @Query("""
            SELECT r FROM ChatRoom r
            WHERE r.type = com.chat.room.domain.RoomType.PUBLIC
              AND (:name IS NULL OR LOWER(r.name) LIKE LOWER(CONCAT('%', :name, '%')))
              AND (SELECT COUNT(m) FROM ChatRoomMember m WHERE m.roomId = r.id) > 0
            ORDER BY r.name ASC
            """)
    Page<ChatRoom> searchPublicRooms(@Param("name") String name, Pageable pageable);
}
