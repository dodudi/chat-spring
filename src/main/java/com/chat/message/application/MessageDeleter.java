package com.chat.message.application;

import java.util.UUID;

public interface MessageDeleter {

    /**
     * 예외: M001(메시지 없음), M002(본인 메시지 아님)
     */
    void deleteMessage(String userId, UUID roomId, Long messageId);
}
