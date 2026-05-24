package com.chat.invitelink.api;

import com.chat.common.ApiResponse;
import com.chat.invitelink.application.InviteLinkJoiner;
import com.chat.invitelink.application.InviteLinkManager;
import com.chat.invitelink.dto.CreateInviteLinkRequest;
import com.chat.invitelink.dto.InviteLinkResponse;
import com.chat.invitelink.dto.JoinByLinkRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class InviteLinkController {

    private final InviteLinkManager inviteLinkManager;
    private final InviteLinkJoiner inviteLinkJoiner;

    @PostMapping("/rooms/{roomId}/invite-links")
    public ResponseEntity<ApiResponse<InviteLinkResponse>> createLink(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID roomId,
            @RequestBody CreateInviteLinkRequest request) {
        InviteLinkResponse response = inviteLinkManager.createLink(jwt.getSubject(), roomId, request);
        URI location = URI.create("/api/v1/invite-links/" + response.id());
        return ResponseEntity.created(location).body(ApiResponse.ok(response));
    }

    @DeleteMapping("/invite-links/{id}")
    public ResponseEntity<Void> deactivateLink(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {
        inviteLinkManager.deactivateLink(jwt.getSubject(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/invite-links/{token}/join")
    public ResponseEntity<Void> joinByLink(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String token,
            @Valid @RequestBody JoinByLinkRequest request) {
        inviteLinkJoiner.joinByLink(jwt.getSubject(), token, request);
        return ResponseEntity.noContent().build();
    }
}
