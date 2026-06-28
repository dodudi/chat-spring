package kr.it.rudy.chat.friend.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FriendRequestRepository extends JpaRepository<FriendRequest, Long> {
    List<FriendRequest> findByReceiverIdAndStatus(Long receiverId, FriendRequestStatus status);
    List<FriendRequest> findByRequesterIdAndStatus(Long requesterId, FriendRequestStatus status);
    boolean existsByRequesterIdAndReceiverIdAndStatus(Long requesterId, Long receiverId, FriendRequestStatus status);
}
