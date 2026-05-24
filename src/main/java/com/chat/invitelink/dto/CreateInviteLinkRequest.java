package com.chat.invitelink.dto;

import jakarta.validation.constraints.FutureOrPresent;

import java.time.OffsetDateTime;

public record CreateInviteLinkRequest(
        @FutureOrPresent OffsetDateTime expiresAt
) {}
