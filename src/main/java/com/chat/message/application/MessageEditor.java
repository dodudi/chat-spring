package com.chat.message.application;

import com.chat.message.dto.EditMessageRequest;
import com.chat.message.dto.MessageResponse;

import java.util.UUID;

public interface MessageEditor {

    /**
     * 예외: M001(메시지 없음), M002(본인 메시지 아님)
     */
    MessageResponse editMessage(String userId, UUID roomId, Long messageId, EditMessageRequest request);
}
