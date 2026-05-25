package com.chat.message.application;

import com.chat.common.exception.AppException;
import com.chat.common.exception.ErrorCode;
import com.chat.message.domain.Message;
import com.chat.message.infrastructure.MessageRepository;
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
class DefaultMessageDeleterTest {

    @InjectMocks
    private DefaultMessageDeleter messageDeleter;

    @Mock private MessageRepository messageRepository;
    @Mock private ChatMessagePublisher chatMessagePublisher;

    @Test
    @DisplayName("메시지 철회 성공 — deletedAt 설정, registerSynchronization 등록")
    void deleteMessage_success() {
        // given
        String userId = "user-1";
        UUID roomId = UUID.randomUUID();
        Long messageId = 1L;
        Message message = Message.create(roomId, userId, 1L, "내용");

        given(messageRepository.findById(messageId)).willReturn(Optional.of(message));

        try (MockedStatic<TransactionSynchronizationManager> txManager =
                     mockStatic(TransactionSynchronizationManager.class)) {
            txManager.when(() -> TransactionSynchronizationManager.registerSynchronization(any()))
                    .thenAnswer(inv -> null);

            // when
            messageDeleter.deleteMessage(userId, roomId, messageId);

            // then
            assertThat(message.getDeletedAt()).isNotNull();
            // afterCommit 내 publishEventToRoom 실행은 트랜잭션 컨텍스트가 없어 검증 불가
            // registerSynchronization 등록 여부만 확인; 실제 발행은 통합 테스트에서 커버
            txManager.verify(() -> TransactionSynchronizationManager.registerSynchronization(any()));
        }
    }

    @Test
    @DisplayName("존재하지 않는 메시지 철회 시 예외 발생")
    void deleteMessage_messageNotFound_throwsException() {
        // given
        UUID roomId = UUID.randomUUID();
        given(messageRepository.findById(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> messageDeleter.deleteMessage("user-1", roomId, 99L))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.MESSAGE_NOT_FOUND);
    }

    @Test
    @DisplayName("다른 채팅방의 메시지 철회 시 예외 발생")
    void deleteMessage_differentRoom_throwsException() {
        // given
        UUID roomId = UUID.randomUUID();
        UUID otherRoomId = UUID.randomUUID();
        Message message = Message.create(otherRoomId, "user-1", 1L, "내용");

        given(messageRepository.findById(1L)).willReturn(Optional.of(message));

        // when & then
        assertThatThrownBy(() -> messageDeleter.deleteMessage("user-1", roomId, 1L))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.MESSAGE_NOT_FOUND);
    }

    @Test
    @DisplayName("본인 메시지가 아닌 경우 철회 시 예외 발생")
    void deleteMessage_notOwner_throwsException() {
        // given
        UUID roomId = UUID.randomUUID();
        Message message = Message.create(roomId, "other-user", 1L, "내용");

        given(messageRepository.findById(1L)).willReturn(Optional.of(message));

        // when & then
        assertThatThrownBy(() -> messageDeleter.deleteMessage("user-1", roomId, 1L))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.MESSAGE_FORBIDDEN);
    }
}
