package com.chat.message.application;

import com.chat.common.exception.AppException;
import com.chat.common.exception.ErrorCode;
import com.chat.message.domain.Message;
import com.chat.message.dto.EditMessageRequest;
import com.chat.message.dto.MessageResponse;
import com.chat.message.infrastructure.MessageRepository;
import com.chat.profile.domain.Profile;
import com.chat.profile.infrastructure.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class DefaultMessageEditor implements MessageEditor {

    private final MessageRepository messageRepository;
    private final ProfileRepository profileRepository;

    @Override
    public MessageResponse editMessage(String userId, UUID roomId, Long messageId, EditMessageRequest request) {
        Message message = messageRepository.findById(messageId)
                .filter(m -> m.getRoomId().equals(roomId))
                .orElseThrow(() -> new AppException(ErrorCode.MESSAGE_NOT_FOUND));

        if (!message.getSenderId().equals(userId)) {
            throw new AppException(ErrorCode.MESSAGE_FORBIDDEN);
        }

        message.edit(request.content());

        String nickname = message.getProfileId() == null ? "" :
                profileRepository.findById(message.getProfileId())
                        .map(Profile::getNickname).orElse("");

        return MessageResponse.of(message, nickname);
    }
}
