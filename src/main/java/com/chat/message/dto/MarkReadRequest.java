package com.chat.message.dto;

import jakarta.validation.constraints.NotNull;

public record MarkReadRequest(
        @NotNull Long lastReadMessageId
) {}
