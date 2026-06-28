package kr.it.rudy.chat.notification.application;

import kr.it.rudy.chat.channel.domain.Channel;
import kr.it.rudy.chat.channel.domain.ChannelRepository;
import kr.it.rudy.chat.common.exception.AuthException;
import kr.it.rudy.chat.common.exception.ErrorCode;
import kr.it.rudy.chat.notification.domain.*;
import kr.it.rudy.chat.notification.dto.NotificationResponse;
import kr.it.rudy.chat.notification.dto.NotificationSettingResponse;
import kr.it.rudy.chat.notification.dto.SaveNotificationSettingRequest;
import kr.it.rudy.chat.server.domain.Server;
import kr.it.rudy.chat.server.domain.ServerRepository;
import kr.it.rudy.chat.user.domain.User;
import kr.it.rudy.chat.user.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class SimpleNotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private NotificationSettingRepository settingRepository;
    @Mock private UserRepository userRepository;
    @Mock private ServerRepository serverRepository;
    @Mock private ChannelRepository channelRepository;

    @InjectMocks
    private SimpleNotificationService notificationService;

    private User user;
    private User other;
    private Notification notification;
    private NotificationSetting setting;

    @BeforeEach
    void setUp() {
        user = User.create("ext-user");
        ReflectionTestUtils.setField(user, "id", 1L);

        other = User.create("ext-other");
        ReflectionTestUtils.setField(other, "id", 2L);

        notification = Notification.create(user, NotificationType.FRIEND_REQUEST, "friend_request", 10L);
        ReflectionTestUtils.setField(notification, "id", 100L);

        setting = NotificationSetting.create(user, null, null, MuteLevel.ALL, null);
        ReflectionTestUtils.setField(setting, "id", 200L);
    }

    @Test
    void getNotifications_전체_알림_목록_반환() {
        // given
        given(userRepository.findByExternalId("ext-user")).willReturn(Optional.of(user));
        given(notificationRepository.findByUserIdOrderByCreatedAtDesc(1L)).willReturn(List.of(notification));

        // when
        List<NotificationResponse> responses = notificationService.getNotifications("ext-user", false);

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).type()).isEqualTo(NotificationType.FRIEND_REQUEST);
        assertThat(responses.get(0).isRead()).isFalse();
    }

    @Test
    void getNotifications_읽지_않은_알림만_반환() {
        // given
        given(userRepository.findByExternalId("ext-user")).willReturn(Optional.of(user));
        given(notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(1L)).willReturn(List.of(notification));

        // when
        List<NotificationResponse> responses = notificationService.getNotifications("ext-user", true);

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).isRead()).isFalse();
    }

    @Test
    void markAsRead_정상_읽음_처리시_isRead_true_반환() {
        // given
        given(userRepository.findByExternalId("ext-user")).willReturn(Optional.of(user));
        given(notificationRepository.findById(100L)).willReturn(Optional.of(notification));

        // when
        NotificationResponse response = notificationService.markAsRead("ext-user", 100L);

        // then
        assertThat(response.isRead()).isTrue();
    }

    @Test
    void markAsRead_본인_알림이_아니면_AuthException_발생() {
        // given
        given(userRepository.findByExternalId("ext-other")).willReturn(Optional.of(other));
        given(notificationRepository.findById(100L)).willReturn(Optional.of(notification));

        // when & then
        assertThatThrownBy(() -> notificationService.markAsRead("ext-other", 100L))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining(ErrorCode.NOTIFICATION_FORBIDDEN.getMessage());
    }

    @Test
    void markAllAsRead_전체_읽음_처리() {
        // given
        given(userRepository.findByExternalId("ext-user")).willReturn(Optional.of(user));

        // when
        notificationService.markAllAsRead("ext-user");

        // then
        then(notificationRepository).should().markAllAsRead(1L);
    }

    @Test
    void getSettings_알림_설정_목록_반환() {
        // given
        given(userRepository.findByExternalId("ext-user")).willReturn(Optional.of(user));
        given(settingRepository.findByUserId(1L)).willReturn(List.of(setting));

        // when
        List<NotificationSettingResponse> responses = notificationService.getSettings("ext-user");

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).muteLevel()).isEqualTo(MuteLevel.ALL);
    }

    @Test
    void saveSetting_전역_설정이_없으면_새로_생성() {
        // given
        given(userRepository.findByExternalId("ext-user")).willReturn(Optional.of(user));
        given(settingRepository.findByUserIdAndServerIsNullAndChannelIsNull(1L)).willReturn(Optional.empty());
        given(settingRepository.save(any())).willReturn(setting);

        // when
        NotificationSettingResponse response = notificationService.saveSetting("ext-user",
                new SaveNotificationSettingRequest(null, null, MuteLevel.ALL, null));

        // then
        assertThat(response.muteLevel()).isEqualTo(MuteLevel.ALL);
        then(settingRepository).should().save(any(NotificationSetting.class));
    }

    @Test
    void saveSetting_전역_설정이_있으면_업데이트() {
        // given
        given(userRepository.findByExternalId("ext-user")).willReturn(Optional.of(user));
        given(settingRepository.findByUserIdAndServerIsNullAndChannelIsNull(1L)).willReturn(Optional.of(setting));
        given(settingRepository.save(any())).willReturn(setting);

        // when
        notificationService.saveSetting("ext-user",
                new SaveNotificationSettingRequest(null, null, MuteLevel.NOTHING, null));

        // then
        assertThat(setting.getMuteLevel()).isEqualTo(MuteLevel.NOTHING);
    }

    @Test
    void saveSetting_서버_설정_저장() {
        // given
        Server server = Server.create(user, "테스트 서버", null, false);
        ReflectionTestUtils.setField(server, "id", 5L);
        NotificationSetting serverSetting = NotificationSetting.create(user, server, null, MuteLevel.MENTIONS_ONLY, null);
        ReflectionTestUtils.setField(serverSetting, "id", 201L);

        given(userRepository.findByExternalId("ext-user")).willReturn(Optional.of(user));
        given(serverRepository.findById(5L)).willReturn(Optional.of(server));
        given(settingRepository.findByUserIdAndServerIdAndChannelIsNull(1L, 5L)).willReturn(Optional.empty());
        given(settingRepository.save(any())).willReturn(serverSetting);

        // when
        NotificationSettingResponse response = notificationService.saveSetting("ext-user",
                new SaveNotificationSettingRequest(5L, null, MuteLevel.MENTIONS_ONLY, null));

        // then
        assertThat(response.muteLevel()).isEqualTo(MuteLevel.MENTIONS_ONLY);
    }

    @Test
    void deleteSetting_정상_삭제() {
        // given
        given(userRepository.findByExternalId("ext-user")).willReturn(Optional.of(user));
        given(settingRepository.findById(200L)).willReturn(Optional.of(setting));

        // when
        notificationService.deleteSetting("ext-user", 200L);

        // then
        then(settingRepository).should().delete(setting);
    }

    @Test
    void deleteSetting_본인_설정이_아니면_AuthException_발생() {
        // given
        given(userRepository.findByExternalId("ext-other")).willReturn(Optional.of(other));
        given(settingRepository.findById(200L)).willReturn(Optional.of(setting));

        // when & then
        assertThatThrownBy(() -> notificationService.deleteSetting("ext-other", 200L))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining(ErrorCode.NOTIFICATION_FORBIDDEN.getMessage());
    }
}
