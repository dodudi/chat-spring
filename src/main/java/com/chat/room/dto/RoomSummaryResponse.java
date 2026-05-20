package com.chat.room.dto;

import com.chat.room.domain.ChatRoom;
import com.chat.room.domain.RoomType;
import com.chat.room.infrastructure.RoomSummaryProjection;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public record RoomSummaryResponse(
        Long id,
        RoomType type,
        String name,
        String dmTargetUserId,
        String lastMessageContent,
        OffsetDateTime lastMessageAt,
        long unreadCount,
        OffsetDateTime updatedAt
) {

    public static RoomSummaryResponse from(RoomSummaryProjection p, String currentUserId) {
        RoomType roomType = RoomType.valueOf(p.getType());
        String dmTarget = roomType == RoomType.DM
                ? (currentUserId.equals(p.getDmUserA()) ? p.getDmUserB() : p.getDmUserA())
                : null;
        return new RoomSummaryResponse(
                p.getId(), roomType, p.getName(), dmTarget,
                p.getLastMessageContent(), toOffsetDateTime(p.getLastMessageAt()),
                p.getUnreadCount(), toOffsetDateTime(p.getUpdatedAt())
        );
    }

    private static OffsetDateTime toOffsetDateTime(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }

    public static RoomSummaryResponse fromNew(ChatRoom room, String currentUserId) {
        String dmTarget = room.getType() == RoomType.DM
                ? (currentUserId.equals(room.getDmUserA()) ? room.getDmUserB() : room.getDmUserA())
                : null;
        return new RoomSummaryResponse(
                room.getId(), room.getType(), room.getName(), dmTarget,
                null, null, 0L, room.getUpdatedAt()
        );
    }
}
