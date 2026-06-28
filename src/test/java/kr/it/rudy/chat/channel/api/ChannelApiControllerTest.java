package kr.it.rudy.chat.channel.api;

import kr.it.rudy.chat.channel.application.ChannelService;
import kr.it.rudy.chat.channel.domain.ChannelType;
import kr.it.rudy.chat.channel.dto.*;
import kr.it.rudy.chat.common.exception.AuthException;
import kr.it.rudy.chat.common.exception.ErrorCode;
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

@WebMvcTest(ChannelApiController.class)
class ChannelApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChannelService channelService;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void createCategory_정상_생성시_201_반환() throws Exception {
        // given
        CategoryResponse response = new CategoryResponse(1L, 10L, "일반", 0, null);
        given(channelService.createCategory(eq("ext-member"), eq(10L), any())).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/servers/10/categories")
                        .with(jwt().jwt(j -> j.claim("sub", "ext-member")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"일반\",\"position\":0}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/channels/categories/1"))
                .andExpect(jsonPath("$.data.name").value("일반"));
    }

    @Test
    void createCategory_name_누락시_400_반환() throws Exception {
        mockMvc.perform(post("/api/v1/servers/10/categories")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"position\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCategory_인증_없으면_401_반환() throws Exception {
        mockMvc.perform(post("/api/v1/servers/10/categories")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"일반\",\"position\":0}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createChannel_정상_생성시_201_반환() throws Exception {
        // given
        ChannelResponse response = new ChannelResponse(1L, 10L, null, ChannelType.TEXT, "general", null, 0, false, 0, null);
        given(channelService.createChannel(eq("ext-member"), eq(10L), any())).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/servers/10/channels")
                        .with(jwt().jwt(j -> j.claim("sub", "ext-member")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"general\",\"type\":\"TEXT\",\"position\":0}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/channels/1"))
                .andExpect(jsonPath("$.data.name").value("general"))
                .andExpect(jsonPath("$.data.type").value("TEXT"));
    }

    @Test
    void createChannel_name_누락시_400_반환() throws Exception {
        mockMvc.perform(post("/api/v1/servers/10/channels")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"type\":\"TEXT\",\"position\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getChannels_채널_목록_200_반환() throws Exception {
        // given
        List<ChannelResponse> channels = List.of(
                new ChannelResponse(1L, 10L, null, ChannelType.TEXT, "general", null, 0, false, 0, null)
        );
        given(channelService.findChannelsByServer(10L)).willReturn(channels);

        // when & then
        mockMvc.perform(get("/api/v1/servers/10/channels")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("general"))
                .andExpect(jsonPath("$.data[0].type").value("TEXT"));
    }

    @Test
    void getChannels_존재하지_않는_서버_404_반환() throws Exception {
        // given
        given(channelService.findChannelsByServer(999L))
                .willThrow(new AuthException(ErrorCode.SERVER_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/api/v1/servers/999/channels")
                        .with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SV001"));
    }

    @Test
    void getChannel_존재하는_채널_200_반환() throws Exception {
        // given
        ChannelResponse response = new ChannelResponse(1L, 10L, null, ChannelType.TEXT, "general", null, 0, false, 0, null);
        given(channelService.findById(1L)).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/channels/1")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("general"));
    }

    @Test
    void getChannel_존재하지_않는_채널_404_반환() throws Exception {
        // given
        given(channelService.findById(999L)).willThrow(new AuthException(ErrorCode.CHANNEL_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/api/v1/channels/999")
                        .with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CH001"));
    }

    @Test
    void updateChannel_정상_수정시_200_반환() throws Exception {
        // given
        ChannelResponse response = new ChannelResponse(1L, 10L, null, ChannelType.TEXT, "renamed", "새 설명", 0, true, 10, null);
        given(channelService.updateChannel(eq("ext-member"), eq(1L), any())).willReturn(response);

        // when & then
        mockMvc.perform(patch("/api/v1/channels/1")
                        .with(jwt().jwt(j -> j.claim("sub", "ext-member")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"renamed\",\"description\":\"새 설명\",\"isNsfw\":true,\"slowmodeSeconds\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("renamed"))
                .andExpect(jsonPath("$.data.isNsfw").value(true));
    }

    @Test
    void updateChannel_멤버_아닌_경우_403_반환() throws Exception {
        // given
        given(channelService.updateChannel(any(), eq(1L), any()))
                .willThrow(new AuthException(ErrorCode.CHANNEL_FORBIDDEN));

        // when & then
        mockMvc.perform(patch("/api/v1/channels/1")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"renamed\",\"isNsfw\":false,\"slowmodeSeconds\":0}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CH003"));
    }

    @Test
    void deleteChannel_정상_삭제시_204_반환() throws Exception {
        // given
        willDoNothing().given(channelService).deleteChannel(eq("ext-member"), eq(1L));

        // when & then
        mockMvc.perform(delete("/api/v1/channels/1")
                        .with(jwt().jwt(j -> j.claim("sub", "ext-member"))))
                .andExpect(status().isNoContent());
    }
}
