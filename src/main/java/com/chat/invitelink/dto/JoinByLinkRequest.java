package com.chat.invitelink.dto;

import jakarta.validation.constraints.NotNull;

public record JoinByLinkRequest(
        @NotNull Long profileId
) {}
