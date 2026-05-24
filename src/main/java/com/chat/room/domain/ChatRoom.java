package com.chat.room.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "chat_rooms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private RoomType type;

    @Column(name = "room_key", nullable = false, unique = true, length = 255)
    private String roomKey;

    @Column(length = 100)
    private String name;

    @Column(length = 255)
    private String password;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public static ChatRoom createDm(String createdBy, String roomKey) {
        ChatRoom room = new ChatRoom();
        room.type = RoomType.DM;
        room.roomKey = roomKey;
        room.createdBy = createdBy;
        return room;
    }

    public static ChatRoom createGroup(String createdBy, String name, String roomKey) {
        ChatRoom room = new ChatRoom();
        room.type = RoomType.GROUP;
        room.roomKey = roomKey;
        room.name = name;
        room.createdBy = createdBy;
        return room;
    }

    public static ChatRoom createPublic(String createdBy, String name, String hashedPassword, String roomKey) {
        ChatRoom room = new ChatRoom();
        room.type = RoomType.PUBLIC;
        room.roomKey = roomKey;
        room.name = name;
        room.password = hashedPassword;
        room.createdBy = createdBy;
        return room;
    }
}
