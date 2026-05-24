package com.chat.presence.dto;

public record PresenceResponse(
        String userId,
        boolean online
) {}
