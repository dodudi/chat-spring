package com.chat.room.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateRoomNameRequest(
        @NotBlank @Size(max = 100) String name
) {}
