package kr.it.rudy.chat.dm.dto;

import jakarta.validation.constraints.NotNull;

public record AddDmParticipantRequest(
        @NotNull Long userId
) {}
