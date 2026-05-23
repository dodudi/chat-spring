package com.chat.room.api;

import com.chat.common.ApiResponse;
import com.chat.room.application.RoomManager;
import com.chat.room.dto.CreateDmRoomRequest;
import com.chat.room.dto.CreateGroupRoomRequest;
import com.chat.room.dto.CreatePublicRoomRequest;
import com.chat.room.dto.DmRoomResponse;
import com.chat.room.dto.PublicRoomResponse;
import com.chat.room.dto.RoomResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomManager roomManager;

    @PostMapping("/dm")
    public ResponseEntity<ApiResponse<DmRoomResponse>> createDmRoom(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateDmRoomRequest request) {
        DmRoomResponse response = roomManager.createDmRoom(jwt.getSubject(), request);
        URI location = URI.create("/api/v1/rooms/" + response.id());
        return ResponseEntity.created(location).body(ApiResponse.ok(response));
    }

    @PostMapping("/group")
    public ResponseEntity<ApiResponse<RoomResponse>> createGroupRoom(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateGroupRoomRequest request) {
        RoomResponse response = roomManager.createGroupRoom(jwt.getSubject(), request);
        URI location = URI.create("/api/v1/rooms/" + response.id());
        return ResponseEntity.created(location).body(ApiResponse.ok(response));
    }

    @PostMapping("/public")
    public ResponseEntity<ApiResponse<PublicRoomResponse>> createPublicRoom(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreatePublicRoomRequest request) {
        PublicRoomResponse response = roomManager.createPublicRoom(jwt.getSubject(), request);
        URI location = URI.create("/api/v1/rooms/" + response.id());
        return ResponseEntity.created(location).body(ApiResponse.ok(response));
    }
}
