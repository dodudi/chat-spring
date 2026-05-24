package com.chat.room.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateRoomPasswordRequest(
        @NotBlank String password
) {}
