package com.chat.message.application;

import com.chat.common.exception.AppException;
import com.chat.common.exception.ErrorCode;
import com.chat.message.domain.Message;
import com.chat.message.domain.ReadCursor;
import com.chat.message.dto.MarkReadRequest;
import com.chat.message.infrastructure.MessageRepository;
import com.chat.message.infrastructure.ReadCursorRepository;
import com.chat.room.infrastructure.ChatRoomMemberRepository;
import com.chat.room.infrastructure.ChatRoomRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DefaultReadCursorUpdaterTest {

    @InjectMocks
    private DefaultReadCursorUpdater readCursorUpdater;

    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private ChatRoomMemberRepository chatRoomMemberRepository;
    @Mock private MessageRepository messageRepository;
    @Mock private ReadCursorRepository readCursorRepository;

    @Test
    @DisplayName("읽음 커서 신규 생성 성공")
    void markRead_신규커서_성공() {
        // given
        String userId = "user-1";
        UUID roomId = UUID.randomUUID();
        Message message = Message.create(roomId, "sender", 1L, "내용");

        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(com.chat.room.domain.ChatRoom.createGroup("owner", "방", "key")));
        given(chatRoomMemberRepository.existsActiveMember(roomId, userId)).willReturn(true);
        given(messageRepository.findById(10L)).willReturn(Optional.of(message));
        given(readCursorRepository.findByRoomIdAndUserId(roomId, userId)).willReturn(Optional.empty());
        given(readCursorRepository.save(any(ReadCursor.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        readCursorUpdater.markRead(userId, roomId, new MarkReadRequest(10L));

        // then
        verify(readCursorRepository).save(any(ReadCursor.class));
    }

    @Test
    @DisplayName("기존 읽음 커서 갱신 성공")
    void markRead_기존커서갱신_성공() {
        // given
        String userId = "user-1";
        UUID roomId = UUID.randomUUID();
        Message message = Message.create(roomId, "sender", 1L, "내용");
        ReadCursor existingCursor = ReadCursor.create(roomId, userId, 5L);

        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(com.chat.room.domain.ChatRoom.createGroup("owner", "방", "key")));
        given(chatRoomMemberRepository.existsActiveMember(roomId, userId)).willReturn(true);
        given(messageRepository.findById(10L)).willReturn(Optional.of(message));
        given(readCursorRepository.findByRoomIdAndUserId(roomId, userId)).willReturn(Optional.of(existingCursor));
        given(readCursorRepository.save(any(ReadCursor.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        readCursorUpdater.markRead(userId, roomId, new MarkReadRequest(10L));

        // then
        assertThat(existingCursor.getLastReadMessageId()).isEqualTo(10L);
        verify(readCursorRepository).save(existingCursor);
    }

    @Test
    @DisplayName("존재하지 않는 방에 읽음 처리 시 예외 발생")
    void markRead_방없음_예외() {
        // given
        UUID roomId = UUID.randomUUID();
        given(chatRoomRepository.findById(roomId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> readCursorUpdater.markRead("user-1", roomId, new MarkReadRequest(10L)))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROOM_NOT_FOUND);
    }

    @Test
    @DisplayName("비활성 멤버가 읽음 처리 시 예외 발생")
    void markRead_비활성멤버_예외() {
        // given
        String userId = "user-1";
        UUID roomId = UUID.randomUUID();
        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(com.chat.room.domain.ChatRoom.createGroup("owner", "방", "key")));
        given(chatRoomMemberRepository.existsActiveMember(roomId, userId)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> readCursorUpdater.markRead(userId, roomId, new MarkReadRequest(10L)))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROOM_MEMBER_NOT_FOUND);
    }

    @Test
    @DisplayName("다른 방의 메시지로 읽음 처리 시 예외 발생")
    void markRead_다른방메시지_예외() {
        // given
        String userId = "user-1";
        UUID roomId = UUID.randomUUID();
        UUID otherRoomId = UUID.randomUUID();
        Message messageInOtherRoom = Message.create(otherRoomId, "sender", 1L, "내용");

        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(com.chat.room.domain.ChatRoom.createGroup("owner", "방", "key")));
        given(chatRoomMemberRepository.existsActiveMember(roomId, userId)).willReturn(true);
        given(messageRepository.findById(10L)).willReturn(Optional.of(messageInOtherRoom));

        // when & then
        assertThatThrownBy(() -> readCursorUpdater.markRead(userId, roomId, new MarkReadRequest(10L)))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.MESSAGE_NOT_FOUND);
    }
}
