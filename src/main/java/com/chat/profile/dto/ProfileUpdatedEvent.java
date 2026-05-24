package com.chat.profile.dto;

import com.chat.common.RoomEvent;

public record ProfileUpdatedEvent(String type, Long profileId, String nickname) implements RoomEvent {

    public static ProfileUpdatedEvent of(Long profileId, String nickname) {
        return new ProfileUpdatedEvent("PROFILE_UPDATED", profileId, nickname);
    }
}
