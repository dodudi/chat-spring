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
@Table(name = "chat_room_members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoomMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false)
    private UUID roomId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "profile_id", nullable = false)
    private Long profileId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberRole role;

    @Column(name = "is_hidden", nullable = false)
    private boolean isHidden;

    @Column(name = "hidden_at")
    private OffsetDateTime hiddenAt;

    @Column(name = "left_at")
    private OffsetDateTime leftAt;

    @Column(name = "kicked_at")
    private OffsetDateTime kickedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public static ChatRoomMember create(UUID roomId, String userId, Long profileId, MemberRole role) {
        ChatRoomMember member = new ChatRoomMember();
        member.roomId = roomId;
        member.userId = userId;
        member.profileId = profileId;
        member.role = role;
        return member;
    }

    public void updateProfileId(Long profileId) {
        this.profileId = profileId;
    }

    public void hide() {
        this.isHidden = true;
        this.hiddenAt = OffsetDateTime.now();
    }

    public void leave() {
        this.role = MemberRole.MEMBER;
        this.leftAt = OffsetDateTime.now();
    }

    public void promoteToOwner() {
        this.role = MemberRole.OWNER;
    }

    public void kick() {
        this.kickedAt = OffsetDateTime.now();
    }
}
