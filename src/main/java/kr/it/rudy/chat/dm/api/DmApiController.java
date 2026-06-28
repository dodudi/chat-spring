package kr.it.rudy.chat.dm.api;

import jakarta.validation.Valid;
import kr.it.rudy.chat.common.response.ApiResponse;
import kr.it.rudy.chat.dm.application.DmService;
import kr.it.rudy.chat.dm.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/dm")
@RequiredArgsConstructor
public class DmApiController {

    private final DmService dmService;

    @PostMapping("/channels/direct")
    public ResponseEntity<ApiResponse<DmChannelResponse>> createDirectChannel(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid CreateDirectDmRequest request
    ) {
        DmChannelResponse response = dmService.createDirectChannel(jwt.getSubject(), request.targetUserId());
        return ResponseEntity
                .created(URI.create("/api/v1/dm/channels/" + response.id()))
                .body(ApiResponse.ok(response));
    }

    @PostMapping("/channels/group")
    public ResponseEntity<ApiResponse<DmChannelResponse>> createGroupChannel(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid CreateGroupDmRequest request
    ) {
        DmChannelResponse response = dmService.createGroupChannel(jwt.getSubject(), request);
        return ResponseEntity
                .created(URI.create("/api/v1/dm/channels/" + response.id()))
                .body(ApiResponse.ok(response));
    }

    @GetMapping("/channels")
    public ResponseEntity<ApiResponse<List<DmChannelResponse>>> getMyChannels(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(ApiResponse.ok(dmService.getMyChannels(jwt.getSubject())));
    }

    @PostMapping("/channels/{channelId}/participants")
    public ResponseEntity<Void> addParticipant(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long channelId,
            @RequestBody @Valid AddDmParticipantRequest request
    ) {
        dmService.addParticipant(jwt.getSubject(), channelId, request.userId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/channels/{channelId}/leave")
    public ResponseEntity<Void> leaveChannel(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long channelId
    ) {
        dmService.leaveChannel(jwt.getSubject(), channelId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/channels/{channelId}/messages")
    public ResponseEntity<ApiResponse<DmMessageResponse>> sendMessage(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long channelId,
            @RequestBody @Valid SendDmMessageRequest request
    ) {
        DmMessageResponse response = dmService.sendMessage(jwt.getSubject(), channelId, request);
        return ResponseEntity
                .created(URI.create("/api/v1/dm/messages/" + response.id()))
                .body(ApiResponse.ok(response));
    }

    @GetMapping("/channels/{channelId}/messages")
    public ResponseEntity<ApiResponse<List<DmMessageResponse>>> findMessages(
            @PathVariable Long channelId,
            @RequestParam(required = false) Long before,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return ResponseEntity.ok(ApiResponse.ok(dmService.findMessages(channelId, before, limit)));
    }

    @PatchMapping("/messages/{messageId}")
    public ResponseEntity<ApiResponse<DmMessageResponse>> editMessage(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long messageId,
            @RequestBody @Valid EditDmMessageRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(dmService.editMessage(jwt.getSubject(), messageId, request)));
    }

    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long messageId
    ) {
        dmService.deleteMessage(jwt.getSubject(), messageId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/channels/{channelId}/read")
    public ResponseEntity<Void> markRead(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long channelId,
            @RequestBody @Valid MarkReadRequest request
    ) {
        dmService.markRead(jwt.getSubject(), channelId, request.lastReadMessageId());
        return ResponseEntity.noContent().build();
    }
}
