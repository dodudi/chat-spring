package com.chat.room.infrastructure;

import com.chat.room.domain.RoomGroupMembership;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomGroupMembershipRepository extends JpaRepository<RoomGroupMembership, Long> {
}
