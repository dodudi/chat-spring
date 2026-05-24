package com.chat.room.api;

import com.chat.common.ApiResponse;
import com.chat.common.dto.PageResponse;
import com.chat.room.application.RoomCreator;
import com.chat.room.application.RoomJoiner;
import com.chat.room.application.RoomReader;
import com.chat.room.application.RoomUpdater;
import com.chat.room.dto.CreateDmRoomRequest;
import com.chat.room.dto.CreateGroupRoomRequest;
import com.chat.room.dto.CreatePublicRoomRequest;
import com.chat.room.dto.DmRoomResponse;
import com.chat.room.dto.JoinPublicRoomRequest;
import com.chat.room.dto.PublicRoomResponse;
import com.chat.room.dto.PublicRoomSummaryResponse;
import com.chat.room.dto.RoomDetailResponse;
import com.chat.room.dto.RoomResponse;
import com.chat.room.dto.RoomSummaryResponse;
import com.chat.room.dto.UpdateRoomNameRequest;
import com.chat.room.dto.UpdateRoomPasswordRequest;
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
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomCreator roomCreator;
    private final RoomReader roomReader;
    private final RoomJoiner roomJoiner;
    private final RoomUpdater roomUpdater;

    @PostMapping("/dm")
    public ResponseEntity<ApiResponse<DmRoomResponse>> createDmRoom(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateDmRoomRequest request) {
        DmRoomResponse response = roomCreator.createDmRoom(jwt.getSubject(), request);
        URI location = URI.create("/api/v1/rooms/" + response.id());
        return ResponseEntity.created(location).body(ApiResponse.ok(response));
    }

    @PostMapping("/group")
    public ResponseEntity<ApiResponse<RoomResponse>> createGroupRoom(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateGroupRoomRequest request) {
        RoomResponse response = roomCreator.createGroupRoom(jwt.getSubject(), request);
        URI location = URI.create("/api/v1/rooms/" + response.id());
        return ResponseEntity.created(location).body(ApiResponse.ok(response));
    }

    @PostMapping("/public")
    public ResponseEntity<ApiResponse<PublicRoomResponse>> createPublicRoom(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreatePublicRoomRequest request) {
        PublicRoomResponse response = roomCreator.createPublicRoom(jwt.getSubject(), request);
        URI location = URI.create("/api/v1/rooms/" + response.id());
        return ResponseEntity.created(location).body(ApiResponse.ok(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RoomSummaryResponse>>> getMyRooms(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Long groupId) {
        return ResponseEntity.ok(ApiResponse.ok(roomReader.getMyRooms(jwt.getSubject(), groupId)));
    }

    @GetMapping("/public")
    public ResponseEntity<ApiResponse<PageResponse<PublicRoomSummaryResponse>>> searchPublicRooms(
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(roomReader.searchPublicRooms(name, page, size)));
    }

    @PatchMapping("/{roomId}")
    public ResponseEntity<ApiResponse<RoomResponse>> updateName(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID roomId,
            @Valid @RequestBody UpdateRoomNameRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(roomUpdater.updateName(jwt.getSubject(), roomId, request)));
    }

    @PutMapping("/{roomId}/password")
    public ResponseEntity<ApiResponse<RoomResponse>> updatePassword(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID roomId,
            @Valid @RequestBody UpdateRoomPasswordRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(roomUpdater.updatePassword(jwt.getSubject(), roomId, request)));
    }

    @DeleteMapping("/{roomId}/password")
    public ResponseEntity<ApiResponse<RoomResponse>> clearPassword(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID roomId) {
        return ResponseEntity.ok(ApiResponse.ok(roomUpdater.clearPassword(jwt.getSubject(), roomId)));
    }

    @PostMapping("/{roomId}/join")
    public ResponseEntity<ApiResponse<RoomResponse>> joinPublicRoom(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID roomId,
            @Valid @RequestBody JoinPublicRoomRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(roomJoiner.joinPublicRoom(jwt.getSubject(), roomId, request)));
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<ApiResponse<RoomDetailResponse>> getRoomDetail(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID roomId) {
        return ResponseEntity.ok(ApiResponse.ok(roomReader.getRoomDetail(jwt.getSubject(), roomId)));
    }
}
