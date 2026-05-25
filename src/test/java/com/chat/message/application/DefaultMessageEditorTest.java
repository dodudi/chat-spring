package com.chat.message.application;

import com.chat.common.exception.AppException;
import com.chat.common.exception.ErrorCode;
import com.chat.message.domain.Message;
import com.chat.message.dto.EditMessageRequest;
import com.chat.message.dto.MessageResponse;
import com.chat.message.infrastructure.MessageRepository;
import com.chat.profile.domain.Profile;
import com.chat.profile.infrastructure.ProfileRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
class DefaultMessageEditorTest {

    @InjectMocks
    private DefaultMessageEditor messageEditor;

    @Mock private MessageRepository messageRepository;
    @Mock private ProfileRepository profileRepository;
    @Mock private ChatMessagePublisher chatMessagePublisher;

    @Test
    @DisplayName("메시지 수정 성공 — content 변경, isEdited true")
    void editMessage_success() {
        // given
        String userId = "user-1";
        UUID roomId = UUID.randomUUID();
        Long messageId = 1L;
        Message message = Message.create(roomId, userId, 1L, "원본 내용");

        given(messageRepository.findById(messageId)).willReturn(Optional.of(message));
        given(profileRepository.findById(1L)).willReturn(Optional.of(Profile.create(userId, "닉네임")));

        try (MockedStatic<TransactionSynchronizationManager> txManager =
                     mockStatic(TransactionSynchronizationManager.class)) {
            txManager.when(() -> TransactionSynchronizationManager.registerSynchronization(any()))
                    .thenAnswer(inv -> null);

            // when
            MessageResponse response = messageEditor.editMessage(userId, roomId, messageId, new EditMessageRequest("수정된 내용"));

            // then
            assertThat(response.content()).isEqualTo("수정된 내용");
            assertThat(response.edited()).isTrue();
            txManager.verify(() -> TransactionSynchronizationManager.registerSynchronization(any()));
        }
    }

    @Test
    @DisplayName("존재하지 않는 메시지 수정 시 예외 발생")
    void editMessage_messageNotFound_throwsException() {
        // given
        UUID roomId = UUID.randomUUID();
        given(messageRepository.findById(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> messageEditor.editMessage("user-1", roomId, 99L, new EditMessageRequest("내용")))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.MESSAGE_NOT_FOUND);
    }

    @Test
    @DisplayName("다른 채팅방의 메시지 수정 시 예외 발생")
    void editMessage_differentRoom_throwsException() {
        // given
        UUID roomId = UUID.randomUUID();
        UUID otherRoomId = UUID.randomUUID();
        Message message = Message.create(otherRoomId, "user-1", 1L, "내용");

        given(messageRepository.findById(1L)).willReturn(Optional.of(message));

        // when & then
        assertThatThrownBy(() -> messageEditor.editMessage("user-1", roomId, 1L, new EditMessageRequest("수정")))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.MESSAGE_NOT_FOUND);
    }

    @Test
    @DisplayName("본인 메시지가 아닌 경우 수정 시 예외 발생")
    void editMessage_notOwner_throwsException() {
        // given
        UUID roomId = UUID.randomUUID();
        Message message = Message.create(roomId, "other-user", 1L, "내용");

        given(messageRepository.findById(1L)).willReturn(Optional.of(message));

        // when & then
        assertThatThrownBy(() -> messageEditor.editMessage("user-1", roomId, 1L, new EditMessageRequest("수정")))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.MESSAGE_FORBIDDEN);
    }
}
