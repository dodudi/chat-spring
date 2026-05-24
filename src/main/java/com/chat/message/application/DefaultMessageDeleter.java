package com.chat.message.application;

import com.chat.common.exception.AppException;
import com.chat.common.exception.ErrorCode;
import com.chat.message.domain.Message;
import com.chat.message.dto.MessageDeletedEvent;
import com.chat.message.infrastructure.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class DefaultMessageDeleter implements MessageDeleter {

    private final MessageRepository messageRepository;
    private final ChatMessagePublisher chatMessagePublisher;

    @Override
    public void deleteMessage(String userId, UUID roomId, Long messageId) {
        Message message = messageRepository.findById(messageId)
                .filter(m -> m.getRoomId().equals(roomId))
                .orElseThrow(() -> new AppException(ErrorCode.MESSAGE_NOT_FOUND));

        if (!message.getSenderId().equals(userId)) {
            throw new AppException(ErrorCode.MESSAGE_FORBIDDEN);
        }

        message.delete();

        MessageDeletedEvent event = MessageDeletedEvent.of(roomId, messageId);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                chatMessagePublisher.publishEventToRoom(roomId, event);
            }
        });
    }
}
