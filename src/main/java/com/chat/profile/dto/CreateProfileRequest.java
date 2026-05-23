package com.chat.profile.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateProfileRequest(
        @NotBlank String nickname
) {}
