package kr.it.rudy.chat.user.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.it.rudy.chat.common.exception.AuthException;
import kr.it.rudy.chat.common.exception.ErrorCode;
import kr.it.rudy.chat.user.application.UserService;
import kr.it.rudy.chat.user.domain.UserStatus;
import kr.it.rudy.chat.user.dto.UpdateStatusRequest;
import kr.it.rudy.chat.user.dto.UserResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserApiController.class)
class UserApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void me_인증된_사용자_200_반환() throws Exception {
        // given
        UserResponse response = new UserResponse(1L, "ext-123", UserStatus.OFFLINE, null);
        given(userService.findOrCreate("ext-123")).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/users/me")
                        .with(jwt().jwt(j -> j.claim("sub", "ext-123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.externalId").value("ext-123"))
                .andExpect(jsonPath("$.data.status").value("OFFLINE"));
    }

    @Test
    void me_인증_없으면_401_반환() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getUser_존재하는_사용자_200_반환() throws Exception {
        // given
        UserResponse response = new UserResponse(1L, "ext-123", UserStatus.ONLINE, null);
        given(userService.findById(1L)).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/users/1")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.externalId").value("ext-123"))
                .andExpect(jsonPath("$.data.status").value("ONLINE"));
    }

    @Test
    void getUser_존재하지_않는_사용자_404_반환() throws Exception {
        // given
        given(userService.findById(99L)).willThrow(new AuthException(ErrorCode.USER_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/api/v1/users/99")
                        .with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("US001"));
    }

    @Test
    void updateStatus_유효한_상태_변경시_200_반환() throws Exception {
        // given
        UserResponse response = new UserResponse(1L, "ext-123", UserStatus.ONLINE, null);
        given(userService.updateStatus("ext-123", UserStatus.ONLINE)).willReturn(response);

        // when & then
        mockMvc.perform(patch("/api/v1/users/me/status")
                        .with(jwt().jwt(j -> j.claim("sub", "ext-123")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateStatusRequest(UserStatus.ONLINE))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ONLINE"));
    }

    @Test
    void updateStatus_status_누락시_400_반환() throws Exception {
        mockMvc.perform(patch("/api/v1/users/me/status")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
