package com.chat.room.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreatePublicRoomRequest(
        @NotBlank String name,
        String password,
        @NotNull Long profileId
) {}
