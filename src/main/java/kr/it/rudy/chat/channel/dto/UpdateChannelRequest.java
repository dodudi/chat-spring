package kr.it.rudy.chat.channel.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateChannelRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 1024) String description,
        boolean isNsfw,
        @Min(0) int slowmodeSeconds
) {
}
