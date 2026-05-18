package com.chat.message.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.OffsetDateTime;

@Entity
@Table(name = "messages")
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "sender_id", nullable = false, length = 255)
    private String senderId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private MessageType type;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public static Message create(Long roomId, String senderId, String content) {
        Message message = new Message();
        message.roomId = roomId;
        message.senderId = senderId;
        message.content = content;
        message.type = MessageType.TEXT;
        message.createdAt = OffsetDateTime.now();
        return message;
    }

    public void delete() {
        this.deletedAt = OffsetDateTime.now();
    }

    public boolean isOwnedBy(String userId) {
        return this.senderId.equals(userId);
    }
}
