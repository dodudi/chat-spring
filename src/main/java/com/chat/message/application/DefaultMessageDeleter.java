package com.chat.message.application;

import com.chat.common.exception.AppException;
import com.chat.common.exception.ErrorCode;
import com.chat.message.domain.Message;
import com.chat.message.infrastructure.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class DefaultMessageDeleter implements MessageDeleter {

    private final MessageRepository messageRepository;

    @Override
    public void deleteMessage(String userId, UUID roomId, Long messageId) {
        Message message = messageRepository.findById(messageId)
                .filter(m -> m.getRoomId().equals(roomId))
                .orElseThrow(() -> new AppException(ErrorCode.MESSAGE_NOT_FOUND));

        if (!message.getSenderId().equals(userId)) {
            throw new AppException(ErrorCode.MESSAGE_FORBIDDEN);
        }

        message.delete();
    }
}
