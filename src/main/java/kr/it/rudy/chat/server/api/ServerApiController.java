package kr.it.rudy.chat.server.api;

import jakarta.validation.Valid;
import kr.it.rudy.chat.common.response.ApiResponse;
import kr.it.rudy.chat.server.application.ServerService;
import kr.it.rudy.chat.server.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/servers")
@RequiredArgsConstructor
public class ServerApiController {

    private final ServerService serverService;

    @PostMapping
    public ResponseEntity<ApiResponse<ServerResponse>> createServer(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid CreateServerRequest request
    ) {
        ServerResponse response = serverService.createServer(jwt.getSubject(), request);
        return ResponseEntity
                .created(URI.create("/api/v1/servers/" + response.id()))
                .body(ApiResponse.ok(response));
    }

    @GetMapping("/{serverId}")
    public ResponseEntity<ApiResponse<ServerResponse>> getServer(@PathVariable Long serverId) {
        return ResponseEntity.ok(ApiResponse.ok(serverService.findById(serverId)));
    }

    @PatchMapping("/{serverId}")
    public ResponseEntity<ApiResponse<ServerResponse>> updateServer(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long serverId,
            @RequestBody @Valid UpdateServerRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(serverService.updateServer(jwt.getSubject(), serverId, request)));
    }

    @DeleteMapping("/{serverId}")
    public ResponseEntity<Void> deleteServer(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long serverId
    ) {
        serverService.deleteServer(jwt.getSubject(), serverId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{serverId}/members")
    public ResponseEntity<ApiResponse<List<ServerMemberResponse>>> getMembers(@PathVariable Long serverId) {
        return ResponseEntity.ok(ApiResponse.ok(serverService.findMembers(serverId)));
    }

    @PostMapping("/join")
    public ResponseEntity<ApiResponse<ServerMemberResponse>> joinServer(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam String code
    ) {
        return ResponseEntity.ok(ApiResponse.ok(serverService.joinServer(jwt.getSubject(), code)));
    }

    @DeleteMapping("/{serverId}/members/me")
    public ResponseEntity<Void> leaveServer(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long serverId
    ) {
        serverService.leaveServer(jwt.getSubject(), serverId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{serverId}/invites")
    public ResponseEntity<ApiResponse<InviteResponse>> createInvite(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long serverId,
            @RequestBody CreateInviteRequest request
    ) {
        InviteResponse response = serverService.createInvite(jwt.getSubject(), serverId, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
