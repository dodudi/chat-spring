package kr.it.rudy.chat.dm.api;

import kr.it.rudy.chat.common.exception.AuthException;
import kr.it.rudy.chat.common.exception.ErrorCode;
import kr.it.rudy.chat.dm.application.DmService;
import kr.it.rudy.chat.dm.domain.DmChannelType;
import kr.it.rudy.chat.dm.dto.DmChannelResponse;
import kr.it.rudy.chat.dm.dto.DmMessageResponse;
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
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DmApiController.class)
class DmApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DmService dmService;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void createDirectChannel_정상_생성시_201_반환() throws Exception {
        // given
        DmChannelResponse response = new DmChannelResponse(10L, DmChannelType.DIRECT, null, null, null);
        given(dmService.createDirectChannel(eq("ext-a"), eq(2L))).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/dm/channels/direct")
                        .with(jwt().jwt(j -> j.claim("sub", "ext-a")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetUserId\":2}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/dm/channels/10"))
                .andExpect(jsonPath("$.data.type").value("DIRECT"));
    }

    @Test
    void createDirectChannel_targetUserId_누락시_400_반환() throws Exception {
        mockMvc.perform(post("/api/v1/dm/channels/direct")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetUserId\":null}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createDirectChannel_인증_없으면_401_반환() throws Exception {
        mockMvc.perform(post("/api/v1/dm/channels/direct")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetUserId\":2}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createGroupChannel_정상_생성시_201_반환() throws Exception {
        // given
        DmChannelResponse response = new DmChannelResponse(20L, DmChannelType.GROUP, "테스트 그룹", null, null);
        given(dmService.createGroupChannel(eq("ext-a"), any())).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/dm/channels/group")
                        .with(jwt().jwt(j -> j.claim("sub", "ext-a")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"테스트 그룹\",\"participantIds\":[2,3]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.type").value("GROUP"))
                .andExpect(jsonPath("$.data.name").value("테스트 그룹"));
    }

    @Test
    void getMyChannels_200_반환() throws Exception {
        // given
        List<DmChannelResponse> responses = List.of(
                new DmChannelResponse(10L, DmChannelType.DIRECT, null, null, null)
        );
        given(dmService.getMyChannels("ext-a")).willReturn(responses);

        // when & then
        mockMvc.perform(get("/api/v1/dm/channels")
                        .with(jwt().jwt(j -> j.claim("sub", "ext-a"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(10));
    }

    @Test
    void addParticipant_1대1_DM이면_400_반환() throws Exception {
        // given
        willThrow(new AuthException(ErrorCode.DM_DIRECT_CANNOT_ADD_PARTICIPANT))
                .given(dmService).addParticipant(any(), eq(10L), eq(2L));

        // when & then
        mockMvc.perform(post("/api/v1/dm/channels/10/participants")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":2}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DM003"));
    }

    @Test
    void leaveChannel_정상_나가기_204_반환() throws Exception {
        // given
        willDoNothing().given(dmService).leaveChannel(eq("ext-a"), eq(10L));

        // when & then
        mockMvc.perform(delete("/api/v1/dm/channels/10/leave")
                        .with(jwt().jwt(j -> j.claim("sub", "ext-a"))))
                .andExpect(status().isNoContent());
    }

    @Test
    void sendMessage_정상_전송시_201_반환() throws Exception {
        // given
        DmMessageResponse response = new DmMessageResponse(100L, 10L, 1L, "ext-a", "안녕하세요", null, false, null);
        given(dmService.sendMessage(eq("ext-a"), eq(10L), any())).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/dm/channels/10/messages")
                        .with(jwt().jwt(j -> j.claim("sub", "ext-a")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"안녕하세요\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/dm/messages/100"))
                .andExpect(jsonPath("$.data.content").value("안녕하세요"));
    }

    @Test
    void sendMessage_참여자가_아니면_403_반환() throws Exception {
        // given
        given(dmService.sendMessage(any(), eq(10L), any()))
                .willThrow(new AuthException(ErrorCode.DM_NOT_PARTICIPANT));

        // when & then
        mockMvc.perform(post("/api/v1/dm/channels/10/messages")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"안녕하세요\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("DM002"));
    }

    @Test
    void findMessages_200_반환() throws Exception {
        // given
        List<DmMessageResponse> responses = List.of(
                new DmMessageResponse(100L, 10L, 1L, "ext-a", "안녕하세요", null, false, null)
        );
        given(dmService.findMessages(eq(10L), isNull(), eq(50))).willReturn(responses);

        // when & then
        mockMvc.perform(get("/api/v1/dm/channels/10/messages")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].content").value("안녕하세요"));
    }

    @Test
    void editMessage_정상_수정시_200_반환() throws Exception {
        // given
        DmMessageResponse response = new DmMessageResponse(100L, 10L, 1L, "ext-a", "수정된 내용", null, true, null);
        given(dmService.editMessage(eq("ext-a"), eq(100L), any())).willReturn(response);

        // when & then
        mockMvc.perform(patch("/api/v1/dm/messages/100")
                        .with(jwt().jwt(j -> j.claim("sub", "ext-a")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"수정된 내용\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("수정된 내용"))
                .andExpect(jsonPath("$.data.isEdited").value(true));
    }

    @Test
    void editMessage_권한_없으면_403_반환() throws Exception {
        // given
        given(dmService.editMessage(any(), eq(100L), any()))
                .willThrow(new AuthException(ErrorCode.DM_MESSAGE_EDIT_FORBIDDEN));

        // when & then
        mockMvc.perform(patch("/api/v1/dm/messages/100")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"수정된 내용\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("DM005"));
    }

    @Test
    void deleteMessage_정상_삭제시_204_반환() throws Exception {
        // given
        willDoNothing().given(dmService).deleteMessage(eq("ext-a"), eq(100L));

        // when & then
        mockMvc.perform(delete("/api/v1/dm/messages/100")
                        .with(jwt().jwt(j -> j.claim("sub", "ext-a"))))
                .andExpect(status().isNoContent());
    }

    @Test
    void markRead_정상_읽음_처리_204_반환() throws Exception {
        // given
        willDoNothing().given(dmService).markRead(eq("ext-a"), eq(10L), eq(100L));

        // when & then
        mockMvc.perform(post("/api/v1/dm/channels/10/read")
                        .with(jwt().jwt(j -> j.claim("sub", "ext-a")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lastReadMessageId\":100}"))
                .andExpect(status().isNoContent());
    }
}
