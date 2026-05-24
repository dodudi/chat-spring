package com.chat.presence.api;

import com.chat.common.ApiResponse;
import com.chat.presence.application.PresenceService;
import com.chat.presence.dto.PresenceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class PresenceController {

    private final PresenceService presenceService;

    @GetMapping("/{userId}/presence")
    public ResponseEntity<ApiResponse<PresenceResponse>> getPresence(
            @PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.ok(
                new PresenceResponse(userId, presenceService.isOnline(userId))));
    }
}
