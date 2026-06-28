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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SimpleNotificationService implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationSettingRepository settingRepository;
    private final UserRepository userRepository;
    private final ServerRepository serverRepository;
    private final ChannelRepository channelRepository;

    @Override
    public List<NotificationResponse> getNotifications(String externalId, boolean unreadOnly) {
        User user = findUser(externalId);
        List<Notification> notifications = unreadOnly
                ? notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(user.getId())
                : notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        return notifications.stream().map(NotificationResponse::from).toList();
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(String externalId, Long notificationId) {
        User user = findUser(externalId);
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new AuthException(ErrorCode.NOTIFICATION_NOT_FOUND));
        if (!notification.isOwnedBy(user)) {
            throw new AuthException(ErrorCode.NOTIFICATION_FORBIDDEN);
        }
        notification.markRead();
        log.info("[NOTIFICATION_READ] userId={} notificationId={}", user.getId(), notificationId);
        return NotificationResponse.from(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead(String externalId) {
        User user = findUser(externalId);
        notificationRepository.markAllAsRead(user.getId());
        log.info("[NOTIFICATION_READ_ALL] userId={}", user.getId());
    }

    @Override
    public List<NotificationSettingResponse> getSettings(String externalId) {
        User user = findUser(externalId);
        return settingRepository.findByUserId(user.getId())
                .stream().map(NotificationSettingResponse::from).toList();
    }

    @Override
    @Transactional
    public NotificationSettingResponse saveSetting(String externalId, SaveNotificationSettingRequest request) {
        User user = findUser(externalId);
        Server server = request.serverId() != null
                ? serverRepository.findById(request.serverId()).orElseThrow(() -> new AuthException(ErrorCode.SERVER_NOT_FOUND))
                : null;
        Channel channel = request.channelId() != null
                ? channelRepository.findById(request.channelId()).orElseThrow(() -> new AuthException(ErrorCode.CHANNEL_NOT_FOUND))
                : null;
        Optional<NotificationSetting> existing = findExistingSetting(user.getId(), request.serverId(), request.channelId());
        NotificationSetting setting;
        if (existing.isPresent()) {
            setting = existing.get();
            setting.update(request.muteLevel(), request.mutedUntil());
        } else {
            setting = NotificationSetting.create(user, server, channel, request.muteLevel(), request.mutedUntil());
        }
        log.info("[NOTIFICATION_SETTING_SAVE] userId={} serverId={} channelId={}", user.getId(), request.serverId(), request.channelId());
        return NotificationSettingResponse.from(settingRepository.save(setting));
    }

    @Override
    @Transactional
    public void deleteSetting(String externalId, Long settingId) {
        User user = findUser(externalId);
        NotificationSetting setting = settingRepository.findById(settingId)
                .orElseThrow(() -> new AuthException(ErrorCode.NOTIFICATION_SETTING_NOT_FOUND));
        if (!setting.isOwnedBy(user)) {
            throw new AuthException(ErrorCode.NOTIFICATION_FORBIDDEN);
        }
        settingRepository.delete(setting);
        log.info("[NOTIFICATION_SETTING_DELETE] userId={} settingId={}", user.getId(), settingId);
    }

    private User findUser(String externalId) {
        return userRepository.findByExternalId(externalId)
                .orElseThrow(() -> new AuthException(ErrorCode.USER_NOT_FOUND));
    }

    private Optional<NotificationSetting> findExistingSetting(Long userId, Long serverId, Long channelId) {
        if (serverId == null && channelId == null) {
            return settingRepository.findByUserIdAndServerIsNullAndChannelIsNull(userId);
        }
        if (serverId != null && channelId == null) {
            return settingRepository.findByUserIdAndServerIdAndChannelIsNull(userId, serverId);
        }
        return settingRepository.findByUserIdAndServerIsNullAndChannelId(userId, channelId);
    }
}
