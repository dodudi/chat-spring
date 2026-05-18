package com.chat.websocket.handler;

import com.chat.common.exception.AppException;
import com.chat.common.exception.ErrorCode;
import com.chat.message.application.MessageService;
import com.chat.message.domain.Message;
import com.chat.websocket.dto.SendMessageRequest;
import com.chat.websocket.dto.MarkReadRequest;
import com.chat.websocket.redis.ChatMessagePublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.Principal;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class StompChatHandlerTest {

    @Mock MessageService messageService;
    @Mock ChatMessagePublisher chatMessagePublisher;
    @InjectMocks StompChatHandler handler;

    private final Principal principal = () -> "user-a";

    @Test
    void sendMessage_메시지_저장_후_Redis_publish() {
        // given
        Message saved = createMessage(1L, 10L, "user-a", "안녕");
        given(messageService.sendMessage("user-a", 10L, "안녕")).willReturn(saved);

        // when
        handler.sendMessage(10L, new SendMessageRequest("안녕"), principal);

        // then
        then(messageService).should().sendMessage("user-a", 10L, "안녕");
        then(chatMessagePublisher).should().publish(eq(10L), any());
    }

    @Test
    void sendMessage_비멤버이면_AppException_전파() {
        // given
        given(messageService.sendMessage(anyString(), anyLong(), anyString()))
                .willThrow(new AppException(ErrorCode.ROOM_ACCESS_DENIED));

        // when & then
        assertThatThrownBy(() -> handler.sendMessage(10L, new SendMessageRequest("안녕"), principal))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROOM_ACCESS_DENIED);
        then(chatMessagePublisher).shouldHaveNoInteractions();
    }

    @Test
    void markRead_messageService에_위임() {
        // when
        handler.markRead(10L, new MarkReadRequest(5L), principal);

        // then
        then(messageService).should().markRead("user-a", 10L, 5L);
    }

    private Message createMessage(Long id, Long roomId, String senderId, String content) {
        Message m = Message.create(roomId, senderId, content);
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
