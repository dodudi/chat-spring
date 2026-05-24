package com.chat.invitation.infrastructure;

import com.chat.invitation.domain.Invitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {

    @Query("SELECT i FROM Invitation i WHERE i.inviteeId = :userId AND i.status = com.chat.invitation.domain.InvitationStatus.PENDING")
    List<Invitation> findPendingByInviteeId(@Param("userId") String userId);

    @Query("SELECT COUNT(i) > 0 FROM Invitation i WHERE i.roomId = :roomId AND i.inviteeId = :inviteeId AND i.status = com.chat.invitation.domain.InvitationStatus.PENDING")
    boolean existsPending(@Param("roomId") UUID roomId, @Param("inviteeId") String inviteeId);
}
