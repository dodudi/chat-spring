package com.chat.invitelink.infrastructure;

import com.chat.invitelink.domain.InviteLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface InviteLinkRepository extends JpaRepository<InviteLink, Long> {

    @Query("SELECT l FROM InviteLink l WHERE l.token = :token")
    Optional<InviteLink> findByToken(@Param("token") String token);

    @Modifying
    @Query("DELETE FROM InviteLink l WHERE l.roomId IN :roomIds")
    void deleteByRoomIdIn(@Param("roomIds") Collection<UUID> roomIds);
}
