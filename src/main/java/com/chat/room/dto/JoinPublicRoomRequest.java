package com.chat.room.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record JoinPublicRoomRequest(
        @NotNull Long profileId,
        @Size(max = 100) String password
) {}
