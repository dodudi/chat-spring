package com.chat.message.application;

import com.chat.message.dto.MessageResponse;

import java.util.UUID;

public interface ChatMessagePublisher {

    void publishToRoom(UUID roomId, MessageResponse message);

    void publishEventToRoom(UUID roomId, Object payload);
}
