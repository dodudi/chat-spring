package kr.it.rudy.chat.channel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kr.it.rudy.chat.channel.domain.ChannelType;

public record CreateChannelRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull ChannelType type,
        @Size(max = 1024) String description,
        Long categoryId,
        @NotNull Integer position
) {
}
