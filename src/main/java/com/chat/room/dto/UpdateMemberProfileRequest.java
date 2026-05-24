package com.chat.room.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateMemberProfileRequest(
        @NotNull Long profileId
) {}
