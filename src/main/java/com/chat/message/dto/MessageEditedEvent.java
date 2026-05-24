package com.chat.message.dto;

import java.util.UUID;

public record MessageEditedEvent(String type, UUID roomId, Long messageId, String content) {

    public static MessageEditedEvent of(UUID roomId, Long messageId, String content) {
        return new MessageEditedEvent("MESSAGE_EDITED", roomId, messageId, content);
    }
}
