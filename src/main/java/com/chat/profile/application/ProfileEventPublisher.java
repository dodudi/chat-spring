package com.chat.profile.application;

import com.chat.profile.dto.ProfileUpdatedEvent;

import java.util.List;
import java.util.UUID;

public interface ProfileEventPublisher {

    void publish(List<UUID> roomIds, ProfileUpdatedEvent event);
}
