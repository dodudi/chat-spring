package kr.it.rudy.chat.dm.dto;

import jakarta.validation.constraints.NotNull;

public record CreateDirectDmRequest(
        @NotNull Long targetUserId
) {}
