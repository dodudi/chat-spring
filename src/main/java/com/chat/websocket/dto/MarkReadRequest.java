package com.chat.websocket.dto;

import jakarta.validation.constraints.NotNull;

public record MarkReadRequest(
        @NotNull Long messageId
) {}
