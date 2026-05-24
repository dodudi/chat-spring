package com.chat.message.api;

import com.chat.common.ApiResponse;
import com.chat.message.application.MessageDeleter;
import com.chat.message.application.MessageEditor;
import com.chat.message.application.MessageReader;
import com.chat.message.application.MessageSender;
import com.chat.message.application.ReadCursorUpdater;
import com.chat.message.dto.EditMessageRequest;
import com.chat.message.dto.MarkReadRequest;
import com.chat.message.dto.MessageCursorResponse;
import com.chat.message.dto.MessageResponse;
import com.chat.message.dto.SendMessageRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rooms/{roomId}/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageSender messageSender;
    private final MessageReader messageReader;
    private final MessageEditor messageEditor;
    private final MessageDeleter messageDeleter;
    private final ReadCursorUpdater readCursorUpdater;

    @PostMapping
    public ResponseEntity<ApiResponse<MessageResponse>> sendMessage(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID roomId,
            @Valid @RequestBody SendMessageRequest request) {
        MessageResponse response = messageSender.sendMessage(jwt.getSubject(), roomId, request);
        URI location = URI.create("/api/v1/rooms/" + roomId + "/messages/" + response.id());
        return ResponseEntity.created(location).body(ApiResponse.ok(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<MessageCursorResponse>> getHistory(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID roomId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "30") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                messageReader.getHistory(jwt.getSubject(), roomId, cursor, size)));
    }

    @PatchMapping("/{messageId}")
    public ResponseEntity<ApiResponse<MessageResponse>> editMessage(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID roomId,
            @PathVariable Long messageId,
            @Valid @RequestBody EditMessageRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                messageEditor.editMessage(jwt.getSubject(), roomId, messageId, request)));
    }

    @DeleteMapping("/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID roomId,
            @PathVariable Long messageId) {
        messageDeleter.deleteMessage(jwt.getSubject(), roomId, messageId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/read")
    public ResponseEntity<Void> markRead(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID roomId,
            @Valid @RequestBody MarkReadRequest request) {
        readCursorUpdater.markRead(jwt.getSubject(), roomId, request);
        return ResponseEntity.noContent().build();
    }
}
