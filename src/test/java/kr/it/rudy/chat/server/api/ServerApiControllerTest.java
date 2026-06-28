package kr.it.rudy.chat.server.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.it.rudy.chat.common.exception.AuthException;
import kr.it.rudy.chat.common.exception.ErrorCode;
import kr.it.rudy.chat.server.application.ServerService;
import kr.it.rudy.chat.server.dto.*;
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

@WebMvcTest(ServerApiController.class)
class ServerApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private ServerService serverService;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void createServer_정상_생성시_201_반환() throws Exception {
        // given
        ServerResponse response = new ServerResponse(1L, 1L, "테스트 서버", "설명", null, null, false, null);
        given(serverService.createServer(eq("ext-123"), any())).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/servers")
                        .with(jwt().jwt(j -> j.claim("sub", "ext-123")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"테스트 서버\",\"description\":\"설명\",\"isPublic\":false}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/servers/1"))
                .andExpect(jsonPath("$.data.name").value("테스트 서버"))
                .andExpect(jsonPath("$.data.ownerId").value(1));
    }

    @Test
    void createServer_name_누락시_400_반환() throws Exception {
        // name이 빈 문자열이면 @NotBlank 위반 → 400
        mockMvc.perform(post("/api/v1/servers")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"description\":\"설명\",\"isPublic\":false}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createServer_인증_없으면_401_반환() throws Exception {
        // CSRF 토큰을 직접 전달해 CSRF 검증을 통과시킨 뒤 인증 실패(401)를 확인
        mockMvc.perform(post("/api/v1/servers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"테스트 서버\",\"description\":\"\",\"isPublic\":false}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getServer_존재하는_서버_200_반환() throws Exception {
        // given
        ServerResponse response = new ServerResponse(1L, 1L, "테스트 서버", null, null, null, false, null);
        given(serverService.findById(1L)).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/servers/1")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("테스트 서버"));
    }

    @Test
    void getServer_존재하지_않는_서버_404_반환() throws Exception {
        // given
        given(serverService.findById(999L)).willThrow(new AuthException(ErrorCode.SERVER_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/api/v1/servers/999")
                        .with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SV001"));
    }

    @Test
    void updateServer_소유자가_수정시_200_반환() throws Exception {
        // given
        ServerResponse response = new ServerResponse(1L, 1L, "수정된 이름", null, null, null, true, null);
        given(serverService.updateServer(eq("ext-123"), eq(1L), any())).willReturn(response);

        // when & then
        mockMvc.perform(patch("/api/v1/servers/1")
                        .with(jwt().jwt(j -> j.claim("sub", "ext-123")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"수정된 이름\",\"isPublic\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("수정된 이름"))
                .andExpect(jsonPath("$.data.isPublic").value(true));
    }

    @Test
    void updateServer_권한_없으면_403_반환() throws Exception {
        // given
        given(serverService.updateServer(any(), eq(1L), any()))
                .willThrow(new AuthException(ErrorCode.SERVER_FORBIDDEN));

        // when & then
        mockMvc.perform(patch("/api/v1/servers/1")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"수정된 이름\",\"isPublic\":false}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("SV004"));
    }

    @Test
    void deleteServer_소유자가_삭제시_204_반환() throws Exception {
        // given
        willDoNothing().given(serverService).deleteServer(eq("ext-123"), eq(1L));

        // when & then
        mockMvc.perform(delete("/api/v1/servers/1")
                        .with(jwt().jwt(j -> j.claim("sub", "ext-123"))))
                .andExpect(status().isNoContent());
    }

    @Test
    void getMembers_멤버_목록_200_반환() throws Exception {
        // given
        List<ServerMemberResponse> members = List.of(
                new ServerMemberResponse(1L, 1L, "ext-123", null, null)
        );
        given(serverService.findMembers(1L)).willReturn(members);

        // when & then
        mockMvc.perform(get("/api/v1/servers/1/members")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].externalId").value("ext-123"));
    }

    @Test
    void joinServer_유효한_코드로_참여시_200_반환() throws Exception {
        // given
        ServerMemberResponse member = new ServerMemberResponse(1L, 2L, "ext-other", null, null);
        given(serverService.joinServer(eq("ext-other"), eq("validcode12345"))).willReturn(member);

        // when & then
        mockMvc.perform(post("/api/v1/servers/join")
                        .param("code", "validcode12345")
                        .with(jwt().jwt(j -> j.claim("sub", "ext-other"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.externalId").value("ext-other"));
    }

    @Test
    void joinServer_만료된_코드_사용시_400_반환() throws Exception {
        // given
        given(serverService.joinServer(any(), eq("expiredcode123")))
                .willThrow(new AuthException(ErrorCode.INVITE_EXPIRED));

        // when & then
        mockMvc.perform(post("/api/v1/servers/join")
                        .param("code", "expiredcode123")
                        .with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SV007"));
    }

    @Test
    void leaveServer_탈퇴시_204_반환() throws Exception {
        // given
        willDoNothing().given(serverService).leaveServer(eq("ext-other"), eq(1L));

        // when & then
        mockMvc.perform(delete("/api/v1/servers/1/members/me")
                        .with(jwt().jwt(j -> j.claim("sub", "ext-other"))))
                .andExpect(status().isNoContent());
    }

    @Test
    void createInvite_초대_생성시_200_반환() throws Exception {
        // given
        InviteResponse invite = new InviteResponse(1L, 1L, "newcode1234567", 10, 0, null, null);
        given(serverService.createInvite(eq("ext-123"), eq(1L), any())).willReturn(invite);

        // when & then
        mockMvc.perform(post("/api/v1/servers/1/invites")
                        .with(jwt().jwt(j -> j.claim("sub", "ext-123")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"maxUses\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("newcode1234567"))
                .andExpect(jsonPath("$.data.maxUses").value(10));
    }
}
