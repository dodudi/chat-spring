package kr.it.rudy.chat.dm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateGroupDmRequest(
        @NotBlank String name,
        String iconUrl,
        @NotEmpty List<@NotNull Long> participantIds
) {}
