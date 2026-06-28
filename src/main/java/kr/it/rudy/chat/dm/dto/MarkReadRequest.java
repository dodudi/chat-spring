package kr.it.rudy.chat.dm.dto;

import jakarta.validation.constraints.NotNull;

public record MarkReadRequest(
        @NotNull Long lastReadMessageId
) {}
