package com.chat.room.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateGroupRoomRequest(
        @NotBlank String name,
        @NotNull Long profileId
) {}
