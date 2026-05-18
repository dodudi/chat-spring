package com.chat.websocket.presence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PresenceController.class)
class PresenceControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean PresenceService presenceService;
    @MockitoBean JwtDecoder jwtDecoder;
    @MockitoBean SimpMessagingTemplate simpMessagingTemplate;

    @Test
    void getPresence_온라인이면_online_true_반환() throws Exception {
        // given
        given(presenceService.isOnline("user-b")).willReturn(true);

        // when & then
        mockMvc.perform(get("/api/v1/users/user-b/presence")
                        .with(jwt().jwt(j -> j.subject("user-a"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.online").value(true));
    }

    @Test
    void getPresence_오프라인이면_online_false_반환() throws Exception {
        // given
        given(presenceService.isOnline("user-b")).willReturn(false);

        // when & then
        mockMvc.perform(get("/api/v1/users/user-b/presence")
                        .with(jwt().jwt(j -> j.subject("user-a"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.online").value(false));
    }

    @Test
    void getPresence_인증_없으면_401_반환() throws Exception {
        mockMvc.perform(get("/api/v1/users/user-b/presence"))
                .andExpect(status().isUnauthorized());
    }
}
