package kr.it.rudy.chat.notification.dto;

import jakarta.validation.constraints.NotNull;
import kr.it.rudy.chat.notification.domain.MuteLevel;

import java.time.Instant;

public record SaveNotificationSettingRequest(
        Long serverId,
        Long channelId,
        @NotNull MuteLevel muteLevel,
        Instant mutedUntil
) {}
