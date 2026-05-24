package com.chat.profile.dto;

import java.util.UUID;

public record ProfileUpdatedEvent(String type, Long profileId, String nickname) {

    public static ProfileUpdatedEvent of(Long profileId, String nickname) {
        return new ProfileUpdatedEvent("PROFILE_UPDATED", profileId, nickname);
    }
}
