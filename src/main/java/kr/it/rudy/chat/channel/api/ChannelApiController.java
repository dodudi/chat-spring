package kr.it.rudy.chat.channel.api;

import jakarta.validation.Valid;
import kr.it.rudy.chat.channel.application.ChannelService;
import kr.it.rudy.chat.channel.dto.*;
import kr.it.rudy.chat.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class ChannelApiController {

    private final ChannelService channelService;

    @PostMapping("/api/v1/servers/{serverId}/categories")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long serverId,
            @RequestBody @Valid CreateCategoryRequest request
    ) {
        CategoryResponse response = channelService.createCategory(jwt.getSubject(), serverId, request);
        return ResponseEntity
                .created(URI.create("/api/v1/channels/categories/" + response.id()))
                .body(ApiResponse.ok(response));
    }

    @PostMapping("/api/v1/servers/{serverId}/channels")
    public ResponseEntity<ApiResponse<ChannelResponse>> createChannel(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long serverId,
            @RequestBody @Valid CreateChannelRequest request
    ) {
        ChannelResponse response = channelService.createChannel(jwt.getSubject(), serverId, request);
        return ResponseEntity
                .created(URI.create("/api/v1/channels/" + response.id()))
                .body(ApiResponse.ok(response));
    }

    @GetMapping("/api/v1/servers/{serverId}/channels")
    public ResponseEntity<ApiResponse<List<ChannelResponse>>> getChannels(@PathVariable Long serverId) {
        return ResponseEntity.ok(ApiResponse.ok(channelService.findChannelsByServer(serverId)));
    }

    @GetMapping("/api/v1/channels/{channelId}")
    public ResponseEntity<ApiResponse<ChannelResponse>> getChannel(@PathVariable Long channelId) {
        return ResponseEntity.ok(ApiResponse.ok(channelService.findById(channelId)));
    }

    @PatchMapping("/api/v1/channels/{channelId}")
    public ResponseEntity<ApiResponse<ChannelResponse>> updateChannel(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long channelId,
            @RequestBody @Valid UpdateChannelRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(channelService.updateChannel(jwt.getSubject(), channelId, request)));
    }

    @DeleteMapping("/api/v1/channels/{channelId}")
    public ResponseEntity<Void> deleteChannel(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long channelId
    ) {
        channelService.deleteChannel(jwt.getSubject(), channelId);
        return ResponseEntity.noContent().build();
    }
}
