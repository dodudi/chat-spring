package com.chat.room.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateGroupRoomRequest(
        @NotBlank @Size(max = 100) String name,
        @NotEmpty List<@NotBlank String> memberIds
) {}
