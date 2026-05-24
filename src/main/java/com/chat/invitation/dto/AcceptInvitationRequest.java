package com.chat.invitation.dto;

import jakarta.validation.constraints.NotNull;

public record AcceptInvitationRequest(
        @NotNull Long profileId
) {}
