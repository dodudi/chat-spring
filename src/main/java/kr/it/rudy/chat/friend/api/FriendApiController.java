package kr.it.rudy.chat.friend.api;

import jakarta.validation.Valid;
import kr.it.rudy.chat.common.response.ApiResponse;
import kr.it.rudy.chat.friend.application.FriendService;
import kr.it.rudy.chat.friend.dto.FriendRequestResponse;
import kr.it.rudy.chat.friend.dto.FriendshipResponse;
import kr.it.rudy.chat.friend.dto.SendFriendRequestRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/friends")
@RequiredArgsConstructor
public class FriendApiController {

    private final FriendService friendService;

    @PostMapping("/requests")
    public ResponseEntity<ApiResponse<FriendRequestResponse>> sendRequest(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid SendFriendRequestRequest request
    ) {
        FriendRequestResponse response = friendService.sendRequest(jwt.getSubject(), request.receiverId());
        return ResponseEntity
                .created(URI.create("/api/v1/friends/requests/" + response.id()))
                .body(ApiResponse.ok(response));
    }

    @GetMapping("/requests/received")
    public ResponseEntity<ApiResponse<List<FriendRequestResponse>>> getReceivedRequests(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(ApiResponse.ok(friendService.getReceivedRequests(jwt.getSubject())));
    }

    @GetMapping("/requests/sent")
    public ResponseEntity<ApiResponse<List<FriendRequestResponse>>> getSentRequests(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(ApiResponse.ok(friendService.getSentRequests(jwt.getSubject())));
    }

    @PostMapping("/requests/{requestId}/accept")
    public ResponseEntity<ApiResponse<FriendRequestResponse>> acceptRequest(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long requestId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(friendService.acceptRequest(jwt.getSubject(), requestId)));
    }

    @PostMapping("/requests/{requestId}/reject")
    public ResponseEntity<ApiResponse<FriendRequestResponse>> rejectRequest(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long requestId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(friendService.rejectRequest(jwt.getSubject(), requestId)));
    }

    @DeleteMapping("/requests/{requestId}")
    public ResponseEntity<Void> cancelRequest(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long requestId
    ) {
        friendService.cancelRequest(jwt.getSubject(), requestId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<FriendshipResponse>>> getFriends(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(ApiResponse.ok(friendService.getFriends(jwt.getSubject())));
    }

    @DeleteMapping("/{friendId}")
    public ResponseEntity<Void> removeFriend(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long friendId
    ) {
        friendService.removeFriend(jwt.getSubject(), friendId);
        return ResponseEntity.noContent().build();
    }
}
