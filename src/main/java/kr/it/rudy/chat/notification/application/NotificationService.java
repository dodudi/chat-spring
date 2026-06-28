package kr.it.rudy.chat.notification.application;

import kr.it.rudy.chat.notification.dto.NotificationResponse;
import kr.it.rudy.chat.notification.dto.NotificationSettingResponse;
import kr.it.rudy.chat.notification.dto.SaveNotificationSettingRequest;

import java.util.List;

public interface NotificationService {
    List<NotificationResponse> getNotifications(String externalId, boolean unreadOnly);

    NotificationResponse markAsRead(String externalId, Long notificationId);

    void markAllAsRead(String externalId);

    List<NotificationSettingResponse> getSettings(String externalId);

    NotificationSettingResponse saveSetting(String externalId, SaveNotificationSettingRequest request);

    void deleteSetting(String externalId, Long settingId);
}
