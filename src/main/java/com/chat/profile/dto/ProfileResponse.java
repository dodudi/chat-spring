package com.chat.profile.dto;

import com.chat.profile.domain.Profile;

import java.time.OffsetDateTime;

public record ProfileResponse(
        Long id,
        String nickname,
        OffsetDateTime createdAt
) {
    public static ProfileResponse from(Profile profile) {
        return new ProfileResponse(profile.getId(), profile.getNickname(), profile.getCreatedAt());
    }
}
