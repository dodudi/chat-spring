package com.chat.group.api;

import com.chat.common.ApiResponse;
import com.chat.group.application.GroupManager;
import com.chat.group.application.GroupReader;
import com.chat.group.application.RoomGroupManager;
import com.chat.group.dto.CreateGroupRequest;
import com.chat.group.dto.GroupResponse;
import com.chat.group.dto.UpdateGroupRequest;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupReader groupReader;
    private final GroupManager groupManager;
    private final RoomGroupManager roomGroupManager;

    @GetMapping
    public ResponseEntity<ApiResponse<List<GroupResponse>>> getMyGroups(
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.ok(groupReader.getMyGroups(jwt.getSubject())));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<GroupResponse>> createGroup(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateGroupRequest request) {
        GroupResponse response = groupManager.createGroup(jwt.getSubject(), request);
        URI location = URI.create("/api/v1/groups/" + response.id());
        return ResponseEntity.created(location).body(ApiResponse.ok(response));
    }

    @PatchMapping("/{groupId}")
    public ResponseEntity<ApiResponse<GroupResponse>> renameGroup(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long groupId,
            @Valid @RequestBody UpdateGroupRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(groupManager.renameGroup(jwt.getSubject(), groupId, request)));
    }

    @DeleteMapping("/{groupId}")
    public ResponseEntity<Void> deleteGroup(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long groupId) {
        groupManager.deleteGroup(jwt.getSubject(), groupId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{groupId}/rooms/{roomId}")
    public ResponseEntity<Void> assignRoom(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long groupId,
            @PathVariable UUID roomId) {
        roomGroupManager.assignRoom(jwt.getSubject(), groupId, roomId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{groupId}/rooms/{roomId}")
    public ResponseEntity<Void> removeRoom(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long groupId,
            @PathVariable UUID roomId) {
        roomGroupManager.removeRoom(jwt.getSubject(), groupId, roomId);
        return ResponseEntity.noContent().build();
    }
}
