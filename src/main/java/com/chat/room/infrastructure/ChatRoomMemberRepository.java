package com.chat.room.infrastructure;

import com.chat.room.domain.ChatRoomMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {

    @Query("SELECT m FROM ChatRoomMember m WHERE m.roomId = :roomId AND m.userId = :userId")
    Optional<ChatRoomMember> findByRoomIdAndUserId(@Param("roomId") UUID roomId, @Param("userId") String userId);

    @Query("SELECT COUNT(m) > 0 FROM ChatRoomMember m WHERE m.roomId = :roomId AND m.userId = :userId AND m.leftAt IS NULL AND m.kickedAt IS NULL")
    boolean existsActiveMember(@Param("roomId") UUID roomId, @Param("userId") String userId);

    @Query("SELECT COUNT(m) > 0 FROM ChatRoomMember m WHERE m.profileId = :profileId")
    boolean existsByProfileId(@Param("profileId") Long profileId);

    @Query("SELECT COUNT(m) FROM ChatRoomMember m WHERE m.roomId = :roomId")
    long countByRoomId(@Param("roomId") UUID roomId);

    @Query("SELECT m FROM ChatRoomMember m WHERE m.roomId = :roomId AND m.userId <> :userId")
    Optional<ChatRoomMember> findOtherMember(@Param("roomId") UUID roomId, @Param("userId") String userId);

    @Query("SELECT m.roomId AS roomId, COUNT(m) AS memberCount FROM ChatRoomMember m WHERE m.roomId IN :roomIds GROUP BY m.roomId")
    List<RoomMemberCountProjection> countByRoomIds(@Param("roomIds") Collection<UUID> roomIds);
}
