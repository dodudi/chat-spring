package com.chat.invitation.api;

import com.chat.common.ApiResponse;
import com.chat.invitation.application.InvitationReader;
import com.chat.invitation.application.InvitationResponder;
import com.chat.invitation.application.InvitationSender;
import com.chat.invitation.dto.AcceptInvitationRequest;
import com.chat.invitation.dto.InvitationResponse;
import com.chat.invitation.dto.SendInvitationRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class InvitationController {

    private final InvitationSender invitationSender;
    private final InvitationResponder invitationResponder;
    private final InvitationReader invitationReader;

    @PostMapping("/rooms/{roomId}/invitations")
    public ResponseEntity<Void> sendInvitation(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID roomId,
            @Valid @RequestBody SendInvitationRequest request) {
        invitationSender.sendInvitation(jwt.getSubject(), roomId, request.inviteeId());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/invitations/{id}/accept")
    public ResponseEntity<Void> accept(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @Valid @RequestBody AcceptInvitationRequest request) {
        invitationResponder.accept(jwt.getSubject(), id, request);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/invitations/{id}/reject")
    public ResponseEntity<Void> reject(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {
        invitationResponder.reject(jwt.getSubject(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/invitations")
    public ResponseEntity<ApiResponse<List<InvitationResponse>>> getInvitations(
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.ok(invitationReader.getPendingInvitations(jwt.getSubject())));
    }
}
