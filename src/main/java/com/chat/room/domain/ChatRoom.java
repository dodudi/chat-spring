package com.chat.room.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "chat_rooms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private RoomType type;

    @Column(length = 100)
    private String name;

    @Column(name = "created_by", nullable = false, length = 255)
    private String createdBy;

    // DM 방 참여자 — 중복 방지를 위해 두 user_id를 항상 정렬된 순서로 저장
    @Column(name = "dm_user_a", length = 255)
    private String dmUserA;

    @Column(name = "dm_user_b", length = 255)
    private String dmUserB;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public static ChatRoom createDm(String creatorId, String targetId) {
        ChatRoom room = new ChatRoom();
        room.type = RoomType.DM;
        room.createdBy = creatorId;
        room.createdAt = OffsetDateTime.now();
        if (creatorId.compareTo(targetId) <= 0) {
            room.dmUserA = creatorId;
            room.dmUserB = targetId;
        } else {
            room.dmUserA = targetId;
            room.dmUserB = creatorId;
        }
        return room;
    }

    public static ChatRoom createGroup(String creatorId, String name) {
        ChatRoom room = new ChatRoom();
        room.type = RoomType.GROUP;
        room.name = name;
        room.createdBy = creatorId;
        room.createdAt = OffsetDateTime.now();
        return room;
    }

    public void updateName(String name) {
        this.name = name;
    }

    // 새 메시지 전송 시 updated_at 갱신 — 방 목록 최신순 정렬 기준
    public void touch() {
        this.updatedAt = OffsetDateTime.now();
    }
}
