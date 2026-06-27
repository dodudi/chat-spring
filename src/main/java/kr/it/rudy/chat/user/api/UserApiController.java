package kr.it.rudy.chat.user.api;

import jakarta.validation.Valid;
import kr.it.rudy.chat.common.response.ApiResponse;
import kr.it.rudy.chat.user.application.UserService;
import kr.it.rudy.chat.user.dto.UpdateStatusRequest;
import kr.it.rudy.chat.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserApiController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.ok(userService.findOrCreate(jwt.getSubject())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(userService.findById(id)));
    }

    @PatchMapping("/me/status")
    public ResponseEntity<ApiResponse<UserResponse>> updateStatus(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid UpdateStatusRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(userService.updateStatus(jwt.getSubject(), request.status())));
    }
}
