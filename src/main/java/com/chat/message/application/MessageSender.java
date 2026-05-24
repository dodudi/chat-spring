package com.chat.message.application;

import com.chat.message.dto.MessageResponse;
import com.chat.message.dto.SendMessageRequest;

import java.util.UUID;

public interface MessageSender {

    /**
     * 예외: R001(방 없음), R008(비활성 멤버)
     */
    MessageResponse sendMessage(String userId, UUID roomId, SendMessageRequest request);
}
