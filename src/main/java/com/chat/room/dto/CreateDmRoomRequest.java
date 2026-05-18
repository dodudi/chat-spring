package com.chat.room.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateDmRoomRequest(
        @NotBlank String targetUserId
) {}
