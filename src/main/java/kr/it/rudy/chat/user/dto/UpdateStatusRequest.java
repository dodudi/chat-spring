package kr.it.rudy.chat.user.dto;

import jakarta.validation.constraints.NotNull;
import kr.it.rudy.chat.user.domain.UserStatus;

public record UpdateStatusRequest(
        @NotNull UserStatus status
) {
}
