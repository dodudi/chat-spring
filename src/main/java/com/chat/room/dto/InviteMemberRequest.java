package com.chat.room.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record InviteMemberRequest(
        @NotEmpty List<@NotBlank String> userIds
) {}
