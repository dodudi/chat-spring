package com.chat.room.domain;

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

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "room_group_memberships")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoomGroupMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false)
    private UUID roomId;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public static RoomGroupMembership create(UUID roomId, Long groupId) {
        RoomGroupMembership membership = new RoomGroupMembership();
        membership.roomId = roomId;
        membership.groupId = groupId;
        return membership;
    }
}
