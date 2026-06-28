package kr.it.rudy.chat.friend.dto;

import kr.it.rudy.chat.friend.domain.FriendRequest;
import kr.it.rudy.chat.friend.domain.FriendRequestStatus;

import java.time.Instant;

public record FriendRequestResponse(
        Long id,
        Long requesterId,
        String requesterExternalId,
        Long receiverId,
        String receiverExternalId,
        FriendRequestStatus status,
        Instant createdAt
) {
    public static FriendRequestResponse from(FriendRequest request) {
        return new FriendRequestResponse(
                request.getId(),
                request.getRequester().getId(),
                request.getRequester().getExternalId(),
                request.getReceiver().getId(),
                request.getReceiver().getExternalId(),
                request.getStatus(),
                request.getCreatedAt()
        );
    }
}
