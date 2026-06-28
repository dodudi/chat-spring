package kr.it.rudy.chat.notification.api;

import jakarta.validation.Valid;
import kr.it.rudy.chat.common.response.ApiResponse;
import kr.it.rudy.chat.notification.application.NotificationService;
import kr.it.rudy.chat.notification.dto.NotificationResponse;
import kr.it.rudy.chat.notification.dto.NotificationSettingResponse;
import kr.it.rudy.chat.notification.dto.SaveNotificationSettingRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class NotificationApiController {

    private final NotificationService notificationService;

    @GetMapping("/api/v1/notifications")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getNotifications(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "false") boolean unreadOnly
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                notificationService.getNotifications(jwt.getSubject(), unreadOnly)));
    }

    @PatchMapping("/api/v1/notifications/{notificationId}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long notificationId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                notificationService.markAsRead(jwt.getSubject(), notificationId)));
    }

    @PatchMapping("/api/v1/notifications/read-all")
    public ResponseEntity<Void> markAllAsRead(
            @AuthenticationPrincipal Jwt jwt
    ) {
        notificationService.markAllAsRead(jwt.getSubject());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/v1/notification-settings")
    public ResponseEntity<ApiResponse<List<NotificationSettingResponse>>> getSettings(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(ApiResponse.ok(notificationService.getSettings(jwt.getSubject())));
    }

    @PutMapping("/api/v1/notification-settings")
    public ResponseEntity<ApiResponse<NotificationSettingResponse>> saveSetting(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid SaveNotificationSettingRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(notificationService.saveSetting(jwt.getSubject(), request)));
    }

    @DeleteMapping("/api/v1/notification-settings/{settingId}")
    public ResponseEntity<Void> deleteSetting(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long settingId
    ) {
        notificationService.deleteSetting(jwt.getSubject(), settingId);
        return ResponseEntity.noContent().build();
    }
}
