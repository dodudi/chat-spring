package com.chat.message.api;

import com.chat.common.ApiResponse;
import com.chat.message.application.MessageService;
import com.chat.message.dto.MessageCursorResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
public class MessageController {

    private final MessageService messageService;

    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<ApiResponse<MessageCursorResponse>> getMessages(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long roomId,
            @RequestParam(required = false) Long before,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
        String userId = jwt.getSubject();
        return ResponseEntity.ok(ApiResponse.ok(messageService.getMessages(userId, roomId, before, size)));
    }

    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long messageId) {
        String userId = jwt.getSubject();
        messageService.deleteMessage(userId, messageId);
        return ResponseEntity.noContent().build();
    }
}
