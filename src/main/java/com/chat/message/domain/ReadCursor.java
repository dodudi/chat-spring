package com.chat.message.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "room_read_cursors")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReadCursor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false)
    private UUID roomId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "last_read_message_id")
    private Long lastReadMessageId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public static ReadCursor create(UUID roomId, String userId, Long lastReadMessageId) {
        ReadCursor cursor = new ReadCursor();
        cursor.roomId = roomId;
        cursor.userId = userId;
        cursor.lastReadMessageId = lastReadMessageId;
        return cursor;
    }

    public void update(Long lastReadMessageId) {
        this.lastReadMessageId = lastReadMessageId;
    }
}
