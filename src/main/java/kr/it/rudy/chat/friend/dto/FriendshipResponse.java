package kr.it.rudy.chat.friend.dto;

import kr.it.rudy.chat.friend.domain.Friendship;
import kr.it.rudy.chat.user.domain.User;

import java.time.Instant;

public record FriendshipResponse(
        Long id,
        Long friendId,
        String friendExternalId,
        Instant createdAt
) {
    public static FriendshipResponse of(Friendship friendship, Long myId) {
        User other = friendship.getUser().getId().equals(myId)
                ? friendship.getFriend()
                : friendship.getUser();
        return new FriendshipResponse(
                friendship.getId(),
                other.getId(),
                other.getExternalId(),
                friendship.getCreatedAt()
        );
    }
}
