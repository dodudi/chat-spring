package com.chat.message.application;

import com.chat.message.domain.Message;
import com.chat.message.dto.MessageCursorResponse;

public interface MessageService {

    MessageCursorResponse getMessages(String userId, Long roomId, Long before, int size);

    void deleteMessage(String userId, Long messageId);

    Message sendMessage(String userId, Long roomId, String content);

    void markRead(String userId, Long roomId, Long messageId);
}
