package com.chat.websocket.dto;

public record NotificationMessage(
        String type,
        Long roomId,
        String roomName,
        String invitedBy
) {}
