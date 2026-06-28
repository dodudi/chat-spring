package kr.it.rudy.chat.dm.dto;

import jakarta.validation.constraints.NotBlank;

public record EditDmMessageRequest(
        @NotBlank String content
) {}
