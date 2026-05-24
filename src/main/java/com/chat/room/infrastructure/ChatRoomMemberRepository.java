package com.chat.room.infrastructure;

import com.chat.room.domain.ChatRoomMember;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    @Query("SELECT COUNT(m) > 0 FROM ChatRoomMember m WHERE m.roomId = :roomId AND m.userId = :userId AND m.role = com.chat.room.domain.MemberRole.OWNER AND m.leftAt IS NULL AND m.kickedAt IS NULL")
    boolean isOwner(@Param("roomId") UUID roomId, @Param("userId") String userId);

    @Query("SELECT COUNT(m) > 0 FROM ChatRoomMember m WHERE m.profileId = :profileId")
    boolean existsByProfileId(@Param("profileId") Long profileId);

    @Query("SELECT COUNT(m) FROM ChatRoomMember m WHERE m.roomId = :roomId")
    long countByRoomId(@Param("roomId") UUID roomId);

    @Query("SELECT COUNT(m) FROM ChatRoomMember m WHERE m.roomId = :roomId AND m.leftAt IS NULL AND m.kickedAt IS NULL")
    long countActiveByRoomId(@Param("roomId") UUID roomId);

    @Query("SELECT m FROM ChatRoomMember m WHERE m.roomId = :roomId AND m.userId <> :userId")
    Optional<ChatRoomMember> findOtherMember(@Param("roomId") UUID roomId, @Param("userId") String userId);

    @Query("""
            SELECT m FROM ChatRoomMember m
            WHERE m.roomId = :roomId AND m.userId <> :userId
              AND m.leftAt IS NULL AND m.kickedAt IS NULL
            ORDER BY m.createdAt ASC
            """)
    List<ChatRoomMember> findActiveMembersExcluding(@Param("roomId") UUID roomId, @Param("userId") String userId, Pageable pageable);

    @Modifying
    @Query("UPDATE ChatRoomMember m SET m.isHidden = false WHERE m.roomId = :roomId AND m.isHidden = true")
    void unhideAll(@Param("roomId") UUID roomId);

    @Modifying
    @Query("DELETE FROM ChatRoomMember m WHERE m.roomId IN :roomIds")
    void deleteByRoomIdIn(@Param("roomIds") Collection<UUID> roomIds);

    @Query("""
            SELECT m.roomId AS roomId, p.nickname AS nickname
            FROM ChatRoomMember m
            JOIN Profile p ON p.id = m.profileId
            WHERE m.roomId IN :roomIds AND m.userId <> :userId AND m.leftAt IS NULL AND m.kickedAt IS NULL
            """)
    List<DmRoomNameProjection> findDmRoomNames(@Param("roomIds") Collection<UUID> roomIds, @Param("userId") String userId);

    @Query("SELECT m.roomId AS roomId, COUNT(m) AS memberCount FROM ChatRoomMember m WHERE m.roomId IN :roomIds GROUP BY m.roomId")
    List<RoomMemberCountProjection> countByRoomIds(@Param("roomIds") Collection<UUID> roomIds);

    @Query("SELECT DISTINCT m.roomId FROM ChatRoomMember m WHERE m.profileId = :profileId AND m.leftAt IS NULL AND m.kickedAt IS NULL")
    List<UUID> findActiveRoomIdsByProfileId(@Param("profileId") Long profileId);
}
