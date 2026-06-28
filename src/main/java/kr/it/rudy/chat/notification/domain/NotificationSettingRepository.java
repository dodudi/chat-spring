package kr.it.rudy.chat.notification.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, Long> {
    List<NotificationSetting> findByUserId(Long userId);

    Optional<NotificationSetting> findByUserIdAndServerIsNullAndChannelIsNull(Long userId);

    Optional<NotificationSetting> findByUserIdAndServerIdAndChannelIsNull(Long userId, Long serverId);

    Optional<NotificationSetting> findByUserIdAndServerIsNullAndChannelId(Long userId, Long channelId);
}
