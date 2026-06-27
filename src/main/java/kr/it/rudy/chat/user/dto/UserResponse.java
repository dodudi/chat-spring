package kr.it.rudy.chat.user.dto;

import kr.it.rudy.chat.user.domain.User;
import kr.it.rudy.chat.user.domain.UserStatus;

import java.time.Instant;

public record UserResponse(
        Long id,
        String externalId,
        UserStatus status,
        Instant createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getExternalId(),
                user.getStatus(),
                user.getCreatedAt()
        );
    }
}
