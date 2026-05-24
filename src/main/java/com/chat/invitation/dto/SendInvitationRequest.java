package com.chat.invitation.dto;

import jakarta.validation.constraints.NotBlank;

public record SendInvitationRequest(
        @NotBlank String inviteeId
) {}
