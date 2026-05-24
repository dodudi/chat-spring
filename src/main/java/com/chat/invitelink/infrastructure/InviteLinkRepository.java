package com.chat.invitelink.infrastructure;

import com.chat.invitelink.domain.InviteLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InviteLinkRepository extends JpaRepository<InviteLink, Long> {

    @Query("SELECT l FROM InviteLink l WHERE l.token = :token")
    Optional<InviteLink> findByToken(@Param("token") String token);
}
