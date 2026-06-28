package kr.it.rudy.chat.message.api;

import jakarta.validation.Valid;
import kr.it.rudy.chat.common.response.ApiResponse;
import kr.it.rudy.chat.message.application.MessageService;
import kr.it.rudy.chat.message.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class MessageApiController {

    private final MessageService messageService;

    @PostMapping("/api/v1/channels/{channelId}/messages")
    public ResponseEntity<ApiResponse<MessageResponse>> sendMessage(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long channelId,
            @RequestBody @Valid SendMessageRequest request
    ) {
        MessageResponse response = messageService.sendMessage(jwt.getSubject(), channelId, request);
        return ResponseEntity
                .created(URI.create("/api/v1/messages/" + response.id()))
                .body(ApiResponse.ok(response));
    }

    @GetMapping("/api/v1/channels/{channelId}/messages")
    public ResponseEntity<ApiResponse<List<MessageResponse>>> getMessages(
            @PathVariable Long channelId,
            @RequestParam(required = false) Long before,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return ResponseEntity.ok(ApiResponse.ok(messageService.findMessages(channelId, before, limit)));
    }

    @PatchMapping("/api/v1/messages/{messageId}")
    public ResponseEntity<ApiResponse<MessageResponse>> editMessage(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long messageId,
            @RequestBody @Valid EditMessageRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(messageService.editMessage(jwt.getSubject(), messageId, request)));
    }

    @DeleteMapping("/api/v1/messages/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long messageId
    ) {
        messageService.deleteMessage(jwt.getSubject(), messageId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/v1/messages/{messageId}/reactions/{emoji}")
    public ResponseEntity<ApiResponse<ReactionResponse>> addReaction(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long messageId,
            @PathVariable String emoji
    ) {
        ReactionResponse response = messageService.addReaction(jwt.getSubject(), messageId, emoji);
        return ResponseEntity
                .created(URI.create("/api/v1/messages/" + messageId + "/reactions/" + emoji))
                .body(ApiResponse.ok(response));
    }

    @DeleteMapping("/api/v1/messages/{messageId}/reactions/{emoji}")
    public ResponseEntity<Void> removeReaction(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long messageId,
            @PathVariable String emoji
    ) {
        messageService.removeReaction(jwt.getSubject(), messageId, emoji);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/v1/channels/{channelId}/pins/{messageId}")
    public ResponseEntity<ApiResponse<PinnedMessageResponse>> pinMessage(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long channelId,
            @PathVariable Long messageId
    ) {
        PinnedMessageResponse response = messageService.pinMessage(jwt.getSubject(), channelId, messageId);
        return ResponseEntity
                .created(URI.create("/api/v1/channels/" + channelId + "/pins/" + messageId))
                .body(ApiResponse.ok(response));
    }

    @DeleteMapping("/api/v1/channels/{channelId}/pins/{messageId}")
    public ResponseEntity<Void> unpinMessage(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long channelId,
            @PathVariable Long messageId
    ) {
        messageService.unpinMessage(jwt.getSubject(), channelId, messageId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/v1/channels/{channelId}/pins")
    public ResponseEntity<ApiResponse<List<PinnedMessageResponse>>> getPinnedMessages(
            @PathVariable Long channelId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(messageService.findPinnedMessages(channelId)));
    }
}
