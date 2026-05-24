package com.chat.profile.application;

import com.chat.common.RoomEvent;

import java.util.List;
import java.util.UUID;

public interface ProfileEventPublisher {

    void publish(List<UUID> roomIds, RoomEvent event);
}
