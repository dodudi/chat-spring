package com.chat.room.infrastructure;

import com.chat.room.domain.RoomGroupMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface RoomGroupMembershipRepository extends JpaRepository<RoomGroupMembership, Long> {

    @Query("SELECT m FROM RoomGroupMembership m WHERE m.roomId = :roomId AND m.groupId = :groupId")
    Optional<RoomGroupMembership> findByRoomIdAndGroupId(@Param("roomId") UUID roomId, @Param("groupId") Long groupId);

    @Modifying
    @Query("DELETE FROM RoomGroupMembership m WHERE m.roomId = :roomId AND m.groupId = :groupId")
    void deleteByRoomIdAndGroupId(@Param("roomId") UUID roomId, @Param("groupId") Long groupId);

    @Modifying
    @Query("DELETE FROM RoomGroupMembership m WHERE m.groupId = :groupId")
    void deleteByGroupId(@Param("groupId") Long groupId);

    @Modifying
    @Query("""
            DELETE FROM RoomGroupMembership m
            WHERE m.roomId = :roomId AND m.groupId IN (
                SELECT g.id FROM UserGroup g WHERE g.userId = :userId
            )
            """)
    void deleteByRoomIdAndUserId(@Param("roomId") UUID roomId, @Param("userId") String userId);

    @Modifying
    @Query("DELETE FROM RoomGroupMembership m WHERE m.roomId IN :roomIds")
    void deleteByRoomIdIn(@Param("roomIds") Collection<UUID> roomIds);
}
