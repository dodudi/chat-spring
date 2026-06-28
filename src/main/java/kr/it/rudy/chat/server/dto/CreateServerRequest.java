package kr.it.rudy.chat.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateServerRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 500) String description,
        boolean isPublic
) {
}
