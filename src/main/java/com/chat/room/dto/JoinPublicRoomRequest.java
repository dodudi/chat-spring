package com.chat.room.dto;

import jakarta.validation.constraints.NotNull;

public record JoinPublicRoomRequest(
        @NotNull Long profileId,
        String password
) {}
