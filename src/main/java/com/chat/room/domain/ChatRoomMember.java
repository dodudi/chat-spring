package com.chat.room.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "chat_room_members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoomMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private ChatRoom room;

    @Column(name = "user_id", nullable = false, length = 255)
    private String userId;

    @Column(name = "joined_at", nullable = false)
    private OffsetDateTime joinedAt;

    @Column(name = "last_read_message_id")
    private Long lastReadMessageId;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public static ChatRoomMember create(ChatRoom room, String userId) {
        ChatRoomMember member = new ChatRoomMember();
        member.room = room;
        member.userId = userId;
        member.joinedAt = OffsetDateTime.now();
        member.active = true;
        member.createdAt = OffsetDateTime.now();
        return member;
    }

    public void updateLastRead(Long messageId) {
        this.lastReadMessageId = messageId;
    }

    public void leave() {
        this.active = false;
    }

    public void rejoin() {
        this.active = true;
        this.joinedAt = OffsetDateTime.now();
    }
}
