package kr.it.rudy.chat.notification.dto;

import kr.it.rudy.chat.notification.domain.MuteLevel;
import kr.it.rudy.chat.notification.domain.NotificationSetting;

import java.time.Instant;

public record NotificationSettingResponse(
        Long id,
        Long serverId,
        Long channelId,
        MuteLevel muteLevel,
        Instant mutedUntil
) {
    public static NotificationSettingResponse from(NotificationSetting setting) {
        return new NotificationSettingResponse(
                setting.getId(),
                setting.getServer() != null ? setting.getServer().getId() : null,
                setting.getChannel() != null ? setting.getChannel().getId() : null,
                setting.getMuteLevel(),
                setting.getMutedUntil()
        );
    }
}
