package com.chat.room.infrastructure;

import com.chat.room.domain.ChatRoomMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {

    @Query("SELECT m FROM ChatRoomMember m WHERE m.roomId = :roomId AND m.userId = :userId")
    Optional<ChatRoomMember> findByRoomIdAndUserId(@Param("roomId") UUID roomId, @Param("userId") String userId);

    @Query("SELECT COUNT(m) > 0 FROM ChatRoomMember m WHERE m.profileId = :profileId")
    boolean existsByProfileId(@Param("profileId") Long profileId);

    @Query("SELECT COUNT(m) FROM ChatRoomMember m WHERE m.roomId = :roomId")
    long countByRoomId(@Param("roomId") UUID roomId);

    @Query("SELECT m FROM ChatRoomMember m WHERE m.roomId = :roomId AND m.userId <> :userId")
    Optional<ChatRoomMember> findOtherMember(@Param("roomId") UUID roomId, @Param("userId") String userId);
}
