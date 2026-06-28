package kr.it.rudy.chat.notification.api;

import kr.it.rudy.chat.common.exception.AuthException;
import kr.it.rudy.chat.common.exception.ErrorCode;
import kr.it.rudy.chat.notification.application.NotificationService;
import kr.it.rudy.chat.notification.domain.MuteLevel;
import kr.it.rudy.chat.notification.domain.NotificationType;
import kr.it.rudy.chat.notification.dto.NotificationResponse;
import kr.it.rudy.chat.notification.dto.NotificationSettingResponse;
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

@WebMvcTest(NotificationApiController.class)
class NotificationApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void getNotifications_전체_알림_목록_200_반환() throws Exception {
        // given
        List<NotificationResponse> responses = List.of(
                new NotificationResponse(100L, NotificationType.FRIEND_REQUEST, "friend_request", 10L, false, null)
        );
        given(notificationService.getNotifications("ext-user", false)).willReturn(responses);

        // when & then
        mockMvc.perform(get("/api/v1/notifications")
                        .with(jwt().jwt(j -> j.claim("sub", "ext-user"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].type").value("FRIEND_REQUEST"))
                .andExpect(jsonPath("$.data[0].isRead").value(false));
    }

    @Test
    void getNotifications_읽지_않은_알림만_200_반환() throws Exception {
        // given
        List<NotificationResponse> responses = List.of(
                new NotificationResponse(100L, NotificationType.MENTION, "message", 20L, false, null)
        );
        given(notificationService.getNotifications("ext-user", true)).willReturn(responses);

        // when & then
        mockMvc.perform(get("/api/v1/notifications")
                        .param("unreadOnly", "true")
                        .with(jwt().jwt(j -> j.claim("sub", "ext-user"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].type").value("MENTION"));
    }

    @Test
    void getNotifications_인증_없으면_401_반환() throws Exception {
        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void markAsRead_정상_읽음_처리_200_반환() throws Exception {
        // given
        NotificationResponse response = new NotificationResponse(100L, NotificationType.FRIEND_REQUEST, "friend_request", 10L, true, null);
        given(notificationService.markAsRead("ext-user", 100L)).willReturn(response);

        // when & then
        mockMvc.perform(patch("/api/v1/notifications/100/read")
                        .with(jwt().jwt(j -> j.claim("sub", "ext-user"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isRead").value(true));
    }

    @Test
    void markAsRead_권한_없으면_403_반환() throws Exception {
        // given
        given(notificationService.markAsRead(any(), eq(100L)))
                .willThrow(new AuthException(ErrorCode.NOTIFICATION_FORBIDDEN));

        // when & then
        mockMvc.perform(patch("/api/v1/notifications/100/read")
                        .with(jwt()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NT003"));
    }

    @Test
    void markAllAsRead_204_반환() throws Exception {
        // given
        willDoNothing().given(notificationService).markAllAsRead("ext-user");

        // when & then
        mockMvc.perform(patch("/api/v1/notifications/read-all")
                        .with(jwt().jwt(j -> j.claim("sub", "ext-user"))))
                .andExpect(status().isNoContent());
    }

    @Test
    void getSettings_알림_설정_목록_200_반환() throws Exception {
        // given
        List<NotificationSettingResponse> responses = List.of(
                new NotificationSettingResponse(200L, null, null, MuteLevel.ALL, null)
        );
        given(notificationService.getSettings("ext-user")).willReturn(responses);

        // when & then
        mockMvc.perform(get("/api/v1/notification-settings")
                        .with(jwt().jwt(j -> j.claim("sub", "ext-user"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].muteLevel").value("ALL"));
    }

    @Test
    void saveSetting_전역_설정_저장_200_반환() throws Exception {
        // given
        NotificationSettingResponse response = new NotificationSettingResponse(200L, null, null, MuteLevel.NOTHING, null);
        given(notificationService.saveSetting(eq("ext-user"), any())).willReturn(response);

        // when & then
        mockMvc.perform(put("/api/v1/notification-settings")
                        .with(jwt().jwt(j -> j.claim("sub", "ext-user")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"muteLevel\":\"NOTHING\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.muteLevel").value("NOTHING"));
    }

    @Test
    void saveSetting_muteLevel_누락시_400_반환() throws Exception {
        mockMvc.perform(put("/api/v1/notification-settings")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"muteLevel\":null}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void saveSetting_인증_없으면_401_반환() throws Exception {
        mockMvc.perform(put("/api/v1/notification-settings")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"muteLevel\":\"ALL\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteSetting_정상_삭제_204_반환() throws Exception {
        // given
        willDoNothing().given(notificationService).deleteSetting(eq("ext-user"), eq(200L));

        // when & then
        mockMvc.perform(delete("/api/v1/notification-settings/200")
                        .with(jwt().jwt(j -> j.claim("sub", "ext-user"))))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteSetting_권한_없으면_403_반환() throws Exception {
        // given
        willThrow(new AuthException(ErrorCode.NOTIFICATION_FORBIDDEN))
                .given(notificationService).deleteSetting(any(), eq(200L));

        // when & then
        mockMvc.perform(delete("/api/v1/notification-settings/200")
                        .with(jwt()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NT003"));
    }
}
