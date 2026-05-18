package com.chat.message.api;

import com.chat.message.application.MessageService;
import com.chat.message.domain.MessageType;
import com.chat.message.dto.MessageCursorResponse;
import com.chat.message.dto.MessageResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MessageController.class)
class MessageControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean MessageService messageService;
    @MockitoBean JwtDecoder jwtDecoder;
    @MockitoBean SimpMessagingTemplate simpMessagingTemplate;

    @Test
    void getMessages_200_반환() throws Exception {
        // given
        MessageResponse msg = new MessageResponse(1L, "user-a", "안녕", MessageType.TEXT, OffsetDateTime.now());
        MessageCursorResponse response = new MessageCursorResponse(List.of(msg), null, false);
        given(messageService.getMessages(any(), eq(1L), isNull(), eq(50))).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/rooms/1/messages")
                        .with(jwt().jwt(j -> j.subject("user-a"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.messages.length()").value(1))
                .andExpect(jsonPath("$.data.hasMore").value(false));
    }

    @Test
    void getMessages_before파라미터_전달() throws Exception {
        // given
        MessageCursorResponse response = new MessageCursorResponse(List.of(), null, false);
        given(messageService.getMessages(any(), eq(1L), eq(5L), eq(10))).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/rooms/1/messages?before=5&size=10")
                        .with(jwt().jwt(j -> j.subject("user-a"))))
                .andExpect(status().isOk());
    }

    @Test
    void getMessages_size가_최댓값_초과이면_400_반환() throws Exception {
        mockMvc.perform(get("/api/v1/rooms/1/messages?size=101")
                        .with(jwt().jwt(j -> j.subject("user-a"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteMessage_204_반환() throws Exception {
        // given
        willDoNothing().given(messageService).deleteMessage(any(), eq(1L));

        // when & then
        mockMvc.perform(delete("/api/v1/messages/1")
                        .with(jwt().jwt(j -> j.subject("user-a"))))
                .andExpect(status().isNoContent());
    }
}
