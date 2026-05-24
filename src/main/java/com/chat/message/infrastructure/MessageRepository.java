package com.chat.message.infrastructure;

import com.chat.message.domain.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("""
            SELECT m FROM Message m
            WHERE m.roomId = :roomId
              AND (:cursor IS NULL OR m.id < :cursor)
              AND (:hiddenAfter IS NULL OR m.createdAt > :hiddenAfter)
            ORDER BY m.id DESC
            """)
    List<Message> findHistory(
            @Param("roomId") UUID roomId,
            @Param("cursor") Long cursor,
            @Param("hiddenAfter") OffsetDateTime hiddenAfter,
            Pageable pageable);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.roomId = :roomId AND (:lastReadId IS NULL OR m.id > :lastReadId)")
    long countUnread(@Param("roomId") UUID roomId, @Param("lastReadId") Long lastReadId);

    @Query("""
            SELECT m.roomId AS roomId, m.content AS content, m.createdAt AS createdAt
            FROM Message m
            WHERE m.roomId IN :roomIds
              AND m.id IN (
                SELECT MAX(m2.id) FROM Message m2 WHERE m2.roomId IN :roomIds GROUP BY m2.roomId
              )
            """)
    List<LastMessageProjection> findLastMessages(@Param("roomIds") Collection<UUID> roomIds);

    @Query("""
            SELECT m.roomId AS roomId, COUNT(m) AS unreadCount
            FROM Message m
            LEFT JOIN ReadCursor c ON c.roomId = m.roomId AND c.userId = :userId
            WHERE m.roomId IN :roomIds
              AND (c.lastReadMessageId IS NULL OR m.id > c.lastReadMessageId)
            GROUP BY m.roomId
            """)
    List<UnreadCountProjection> countUnreadByRoomIds(
            @Param("roomIds") Collection<UUID> roomIds,
            @Param("userId") String userId);
}
