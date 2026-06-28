package kr.it.rudy.chat.message.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import kr.it.rudy.chat.message.domain.MessageType;

public record SendMessageRequest(
        @NotBlank @Size(max = 4000) String content,
        MessageType type,
        Long parentMessageId
) {
}
