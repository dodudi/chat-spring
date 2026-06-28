package kr.it.rudy.chat.message.api;

import kr.it.rudy.chat.common.exception.AuthException;
import kr.it.rudy.chat.common.exception.ErrorCode;
import kr.it.rudy.chat.message.application.MessageService;
import kr.it.rudy.chat.message.domain.MessageType;
import kr.it.rudy.chat.message.dto.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MessageApiController.class)
class MessageApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MessageService messageService;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void sendMessage_정상_전송시_201_반환() throws Exception {
        // given
        MessageResponse response = new MessageResponse(1L, 20L, 1L, "ext-sender", "안녕하세요",
                MessageType.DEFAULT, null, false, null, null);
        given(messageService.sendMessage(eq("ext-sender"), eq(20L), any())).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/channels/20/messages")
                        .with(jwt().jwt(j -> j.claim("sub", "ext-sender")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"안녕하세요\",\"type\":\"DEFAULT\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/messages/1"))
                .andExpect(jsonPath("$.data.content").value("안녕하세요"))
                .andExpect(jsonPath("$.data.type").value("DEFAULT"));
    }

    @Test
    void sendMessage_content_누락시_400_반환() throws Exception {
        mockMvc.perform(post("/api/v1/channels/20/messages")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"\",\"type\":\"DEFAULT\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sendMessage_인증_없으면_401_반환() throws Exception {
        mockMvc.perform(post("/api/v1/channels/20/messages")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"안녕하세요\",\"type\":\"DEFAULT\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMessages_메시지_목록_200_반환() throws Exception {
        // given
        List<MessageResponse> messages = List.of(
                new MessageResponse(1L, 20L, 1L, "ext-sender", "안녕하세요", MessageType.DEFAULT, null, false, null, null)
        );
        given(messageService.findMessages(eq(20L), isNull(), eq(50))).willReturn(messages);

        // when & then
        mockMvc.perform(get("/api/v1/channels/20/messages")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].content").value("안녕하세요"));
    }

    @Test
    void getMessages_커서_지정시_200_반환() throws Exception {
        // given
        given(messageService.findMessages(eq(20L), eq(100L), eq(50))).willReturn(List.of());

        // when & then
        mockMvc.perform(get("/api/v1/channels/20/messages")
                        .param("before", "100")
                        .with(jwt()))
                .andExpect(status().isOk());
    }

    @Test
    void getMessages_존재하지_않는_채널_404_반환() throws Exception {
        // given
        given(messageService.findMessages(eq(999L), isNull(), eq(50)))
                .willThrow(new AuthException(ErrorCode.CHANNEL_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/api/v1/channels/999/messages")
                        .with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CH001"));
    }

    @Test
    void editMessage_정상_수정시_200_반환() throws Exception {
        // given
        MessageResponse response = new MessageResponse(1L, 20L, 1L, "ext-sender", "수정된 내용",
                MessageType.DEFAULT, null, true, null, null);
        given(messageService.editMessage(eq("ext-sender"), eq(1L), any())).willReturn(response);

        // when & then
        mockMvc.perform(patch("/api/v1/messages/1")
                        .with(jwt().jwt(j -> j.claim("sub", "ext-sender")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"수정된 내용\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("수정된 내용"))
                .andExpect(jsonPath("$.data.isEdited").value(true));
    }

    @Test
    void editMessage_권한_없으면_403_반환() throws Exception {
        // given
        given(messageService.editMessage(any(), eq(1L), any()))
                .willThrow(new AuthException(ErrorCode.MESSAGE_EDIT_FORBIDDEN));

        // when & then
        mockMvc.perform(patch("/api/v1/messages/1")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"수정된 내용\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("MS002"));
    }

    @Test
    void deleteMessage_정상_삭제시_204_반환() throws Exception {
        // given
        willDoNothing().given(messageService).deleteMessage(eq("ext-sender"), eq(1L));

        // when & then
        mockMvc.perform(delete("/api/v1/messages/1")
                        .with(jwt().jwt(j -> j.claim("sub", "ext-sender"))))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteMessage_권한_없으면_403_반환() throws Exception {
        // given
        given(messageService.editMessage(any(), eq(1L), any()))
                .willThrow(new AuthException(ErrorCode.MESSAGE_DELETE_FORBIDDEN));

        mockMvc.perform(delete("/api/v1/messages/1")
                        .with(jwt()))
                .andExpect(status().isNoContent());
    }

    @Test
    void addReaction_정상_추가시_201_반환() throws Exception {
        // given
        ReactionResponse response = new ReactionResponse(1L, 1L, 1L, "👍", null);
        given(messageService.addReaction(eq("ext-sender"), eq(1L), eq("👍"))).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/messages/1/reactions/👍")
                        .with(jwt().jwt(j -> j.claim("sub", "ext-sender"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.emoji").value("👍"));
    }

    @Test
    void addReaction_중복_반응시_409_반환() throws Exception {
        // given
        given(messageService.addReaction(any(), eq(1L), eq("👍")))
                .willThrow(new AuthException(ErrorCode.REACTION_ALREADY_EXISTS));

        // when & then
        mockMvc.perform(post("/api/v1/messages/1/reactions/👍")
                        .with(jwt()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MS004"));
    }

    @Test
    void removeReaction_정상_제거시_204_반환() throws Exception {
        // given
        willDoNothing().given(messageService).removeReaction(eq("ext-sender"), eq(1L), eq("👍"));

        // when & then
        mockMvc.perform(delete("/api/v1/messages/1/reactions/👍")
                        .with(jwt().jwt(j -> j.claim("sub", "ext-sender"))))
                .andExpect(status().isNoContent());
    }

    @Test
    void pinMessage_정상_고정시_201_반환() throws Exception {
        // given
        PinnedMessageResponse response = new PinnedMessageResponse(1L, 20L, 1L, 1L, null);
        given(messageService.pinMessage(eq("ext-sender"), eq(20L), eq(1L))).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/channels/20/pins/1")
                        .with(jwt().jwt(j -> j.claim("sub", "ext-sender"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.channelId").value(20))
                .andExpect(jsonPath("$.data.messageId").value(1));
    }

    @Test
    void pinMessage_이미_고정된_경우_409_반환() throws Exception {
        // given
        given(messageService.pinMessage(any(), eq(20L), eq(1L)))
                .willThrow(new AuthException(ErrorCode.PIN_ALREADY_EXISTS));

        // when & then
        mockMvc.perform(post("/api/v1/channels/20/pins/1")
                        .with(jwt()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MS006"));
    }

    @Test
    void unpinMessage_정상_해제시_204_반환() throws Exception {
        // given
        willDoNothing().given(messageService).unpinMessage(eq("ext-sender"), eq(20L), eq(1L));

        // when & then
        mockMvc.perform(delete("/api/v1/channels/20/pins/1")
                        .with(jwt().jwt(j -> j.claim("sub", "ext-sender"))))
                .andExpect(status().isNoContent());
    }

    @Test
    void getPinnedMessages_고정_메시지_목록_200_반환() throws Exception {
        // given
        List<PinnedMessageResponse> pins = List.of(
                new PinnedMessageResponse(1L, 20L, 1L, 1L, null)
        );
        given(messageService.findPinnedMessages(20L)).willReturn(pins);

        // when & then
        mockMvc.perform(get("/api/v1/channels/20/pins")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].messageId").value(1));
    }
}
