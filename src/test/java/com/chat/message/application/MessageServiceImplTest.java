package com.chat.message.application;

import com.chat.common.exception.AppException;
import com.chat.common.exception.ErrorCode;
import com.chat.message.domain.Message;
import com.chat.message.dto.MessageCursorResponse;
import com.chat.message.infrastructure.MessageRepository;
import com.chat.room.domain.ChatRoomMember;
import com.chat.room.infrastructure.ChatRoomMemberRepository;
import com.chat.room.infrastructure.ChatRoomRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class MessageServiceImplTest {

    @Mock MessageRepository messageRepository;
    @Mock ChatRoomMemberRepository chatRoomMemberRepository;
    @Mock ChatRoomRepository chatRoomRepository;
    @Mock EntityManager entityManager;
    @InjectMocks MessageServiceImpl messageService;

    @Test
    void getMessages_비멤버이면_R002_예외() {
        // given
        given(chatRoomMemberRepository.existsByRoom_IdAndUserId(1L, "user-a")).willReturn(false);

        // when & then
        assertThatThrownBy(() -> messageService.getMessages("user-a", 1L, null, 50))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROOM_ACCESS_DENIED);
    }

    @Test
    void getMessages_before없으면_최신순_조회() {
        // given
        given(chatRoomMemberRepository.existsByRoom_IdAndUserId(1L, "user-a")).willReturn(true);
        Message m1 = createMessage(1L, "첫 번째");
        Message m2 = createMessage(2L, "두 번째");
        given(messageRepository.findByRoomIdOrderByIdDesc(eq(1L), any(Pageable.class)))
                .willReturn(List.of(m2, m1));

        // when
        MessageCursorResponse result = messageService.getMessages("user-a", 1L, null, 50);

        // then
        assertThat(result.messages()).hasSize(2);
        assertThat(result.hasMore()).isFalse();
        assertThat(result.nextCursor()).isNull();
    }

    @Test
    void getMessages_size보다_많으면_hasMore_true() {
        // given — size=2, fetch 3 (size+1)
        given(chatRoomMemberRepository.existsByRoom_IdAndUserId(1L, "user-a")).willReturn(true);
        Message m3 = createMessage(3L, "세 번째");
        Message m2 = createMessage(2L, "두 번째");
        Message m1 = createMessage(1L, "첫 번째");
        given(messageRepository.findByRoomIdOrderByIdDesc(eq(1L), any(Pageable.class)))
                .willReturn(List.of(m3, m2, m1));

        // when
        MessageCursorResponse result = messageService.getMessages("user-a", 1L, null, 2);

        // then
        assertThat(result.messages()).hasSize(2);
        assertThat(result.hasMore()).isTrue();
        assertThat(result.nextCursor()).isEqualTo(2L); // 반환된 page 내 가장 오래된 메시지 ID
    }

    @Test
    void getMessages_before있으면_커서_기반_조회() {
        // given
        given(chatRoomMemberRepository.existsByRoom_IdAndUserId(1L, "user-a")).willReturn(true);
        Message m1 = createMessage(1L, "첫 번째");
        given(messageRepository.findByRoomIdAndIdLessThanOrderByIdDesc(eq(1L), eq(3L), any(Pageable.class)))
                .willReturn(List.of(m1));

        // when
        MessageCursorResponse result = messageService.getMessages("user-a", 1L, 3L, 50);

        // then
        assertThat(result.messages()).hasSize(1);
        assertThat(result.hasMore()).isFalse();
    }

    @Test
    void deleteMessage_메시지가_없으면_M001_예외() {
        // given
        given(messageRepository.findById(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> messageService.deleteMessage("user-a", 99L))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.MESSAGE_NOT_FOUND);
    }

    @Test
    void deleteMessage_타인_메시지이면_M002_예외() {
        // given
        Message message = createMessage(1L, "내용");
        given(messageRepository.findById(1L)).willReturn(Optional.of(message));

        // when & then — message sender is "user-a", deleter is "user-b"
        assertThatThrownBy(() -> messageService.deleteMessage("user-b", 1L))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.MESSAGE_DELETE_DENIED);
    }

    @Test
    void deleteMessage_본인_메시지이면_삭제_처리() {
        // given
        Message message = createMessage(1L, "내용");
        given(messageRepository.findById(1L)).willReturn(Optional.of(message));

        // when
        messageService.deleteMessage("user-a", 1L);

        // then
        assertThat(message.getDeletedAt()).isNotNull();
        then(entityManager).should().flush();
        then(entityManager).should().clear();
    }

    @Test
    void sendMessage_비멤버이면_R002_예외() {
        // given
        given(chatRoomMemberRepository.existsByRoom_IdAndUserId(1L, "user-a")).willReturn(false);

        // when & then
        assertThatThrownBy(() -> messageService.sendMessage("user-a", 1L, "내용"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROOM_ACCESS_DENIED);
    }

    @Test
    void sendMessage_멤버이면_메시지_저장_및_방_touch() {
        // given
        given(chatRoomMemberRepository.existsByRoom_IdAndUserId(1L, "user-a")).willReturn(true);
        Message message = createMessage(1L, "내용");
        given(messageRepository.save(any(Message.class))).willReturn(message);
        given(chatRoomRepository.findById(1L)).willReturn(Optional.empty()); // room.touch() — no-op if absent

        // when
        Message result = messageService.sendMessage("user-a", 1L, "내용");

        // then
        assertThat(result).isEqualTo(message);
        then(messageRepository).should().save(any(Message.class));
    }

    @Test
    void markRead_비활성_멤버이면_R002_예외() {
        // given
        ChatRoomMember inactive = ChatRoomMember.create(createRoom(), "user-a");
        inactive.leave();
        given(chatRoomMemberRepository.findByRoom_IdAndUserId(1L, "user-a"))
                .willReturn(Optional.of(inactive));

        // when & then
        assertThatThrownBy(() -> messageService.markRead("user-a", 1L, 10L))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROOM_ACCESS_DENIED);
    }

    @Test
    void markRead_활성_멤버이면_lastReadMessageId_업데이트() {
        // given
        ChatRoomMember member = ChatRoomMember.create(createRoom(), "user-a");
        given(chatRoomMemberRepository.findByRoom_IdAndUserId(1L, "user-a"))
                .willReturn(Optional.of(member));

        // when
        messageService.markRead("user-a", 1L, 10L);

        // then
        assertThat(member.getLastReadMessageId()).isEqualTo(10L);
    }

    private com.chat.room.domain.ChatRoom createRoom() {
        return com.chat.room.domain.ChatRoom.createGroup("user-a", "테스트방");
    }

    private Message createMessage(Long id, String content) {
        Message m = Message.create(1L, "user-a", content);
        // id는 reflection 없이 테스트하기 위해 실제 id 세팅 불필요 — 커서 테스트 외 나머지에서만 ID 필요
        try {
            var f = Message.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(m, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return m;
    }
}
