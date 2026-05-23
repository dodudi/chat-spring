package com.chat.room.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateDmRoomRequest(
        @NotBlank String targetUserId,
        @NotNull Long profileId
) {}
