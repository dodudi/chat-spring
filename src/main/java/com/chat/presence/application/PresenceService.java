package com.chat.presence.application;

public interface PresenceService {

    void heartbeat(String userId);

    void offline(String userId);

    boolean isOnline(String userId);
}
