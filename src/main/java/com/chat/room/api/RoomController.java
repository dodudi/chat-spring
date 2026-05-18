package com.chat.room.api;

import com.chat.common.ApiResponse;
import com.chat.room.application.RoomService;
import com.chat.room.dto.CreateDmRoomRequest;
import com.chat.room.dto.CreateGroupRoomRequest;
import com.chat.room.dto.InviteMemberRequest;
import com.chat.room.dto.RoomSummaryResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @PostMapping("/direct")
    public ResponseEntity<ApiResponse<RoomSummaryResponse>> createOrGetDmRoom(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateDmRoomRequest request) {
        String userId = jwt.getSubject();
        return ResponseEntity.ok(ApiResponse.ok(roomService.createOrGetDmRoom(userId, request)));
    }

    @PostMapping("/group")
    public ResponseEntity<ApiResponse<RoomSummaryResponse>> createGroupRoom(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateGroupRoomRequest request) {
        String userId = jwt.getSubject();
        RoomSummaryResponse response = roomService.createGroupRoom(userId, request);
        URI location = URI.create("/api/v1/rooms/" + response.id());
        return ResponseEntity.created(location).body(ApiResponse.ok(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RoomSummaryResponse>>> getMyRooms(
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        return ResponseEntity.ok(ApiResponse.ok(roomService.getMyRooms(userId)));
    }

    @PostMapping("/{roomId}/members")
    public ResponseEntity<ApiResponse<Void>> inviteMembers(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long roomId,
            @Valid @RequestBody InviteMemberRequest request) {
        String userId = jwt.getSubject();
        roomService.inviteMembers(userId, roomId, request);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @DeleteMapping("/{roomId}/members/me")
    public ResponseEntity<Void> leaveRoom(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long roomId) {
        String userId = jwt.getSubject();
        roomService.leaveRoom(userId, roomId);
        return ResponseEntity.noContent().build();
    }
}
