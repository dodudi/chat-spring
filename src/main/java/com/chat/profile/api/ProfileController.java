package com.chat.profile.api;

import com.chat.common.ApiResponse;
import com.chat.profile.application.ProfileManager;
import com.chat.profile.dto.CreateProfileRequest;
import com.chat.profile.dto.ProfileResponse;
import com.chat.profile.dto.UpdateProfileRequest;
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

@RestController
@RequestMapping("/api/v1/profiles")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileManager profileManager;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProfileResponse>>> getMyProfiles(
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.ok(profileManager.getMyProfiles(jwt.getSubject())));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProfileResponse>> createProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateProfileRequest request) {
        ProfileResponse response = profileManager.createProfile(jwt.getSubject(), request);
        URI location = URI.create("/api/v1/profiles/" + response.id());
        return ResponseEntity.created(location).body(ApiResponse.ok(response));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateNickname(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                profileManager.updateNickname(jwt.getSubject(), id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProfile(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {
        profileManager.deleteProfile(jwt.getSubject(), id);
        return ResponseEntity.noContent().build();
    }
}
