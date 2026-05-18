package com.chat.room.infrastructure;

import com.chat.room.domain.ChatRoomMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {

    @Query(value = """
            SELECT
                cr.id,
                cr.type,
                cr.name,
                cr.dm_user_a,
                cr.dm_user_b,
                cr.updated_at,
                (SELECT m.content FROM messages m
                 WHERE m.room_id = cr.id AND m.deleted_at IS NULL
                 ORDER BY m.id DESC LIMIT 1) AS last_message_content,
                (SELECT m.created_at FROM messages m
                 WHERE m.room_id = cr.id AND m.deleted_at IS NULL
                 ORDER BY m.id DESC LIMIT 1) AS last_message_at,
                COUNT(CASE WHEN m2.id > COALESCE(crm.last_read_message_id, 0) THEN 1 END) AS unread_count
            FROM chat_room_members crm
            JOIN chat_rooms cr ON cr.id = crm.room_id
            LEFT JOIN messages m2 ON m2.room_id = cr.id AND m2.deleted_at IS NULL
            WHERE crm.user_id = :userId AND crm.is_active = true
            GROUP BY cr.id, cr.type, cr.name, cr.dm_user_a, cr.dm_user_b,
                     cr.updated_at, crm.last_read_message_id
            ORDER BY cr.updated_at DESC
            """, nativeQuery = true)
    List<RoomSummaryProjection> findMyRoomsWithUnread(@Param("userId") String userId);

    @Query("SELECT m FROM ChatRoomMember m WHERE m.room.id = :roomId AND m.userId = :userId")
    Optional<ChatRoomMember> findMember(@Param("roomId") Long roomId, @Param("userId") String userId);

    @Query("SELECT COUNT(m) > 0 FROM ChatRoomMember m WHERE m.room.id = :roomId AND m.userId = :userId AND m.active = true")
    boolean isActiveMember(@Param("roomId") Long roomId, @Param("userId") String userId);

    @Query("SELECT m FROM ChatRoomMember m WHERE m.room.id = :roomId AND m.active = true")
    List<ChatRoomMember> findActiveMembers(@Param("roomId") Long roomId);
}
