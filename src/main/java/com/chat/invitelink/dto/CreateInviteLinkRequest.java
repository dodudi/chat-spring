package com.chat.invitelink.dto;

import java.time.OffsetDateTime;

public record CreateInviteLinkRequest(
        OffsetDateTime expiresAt
) {}
