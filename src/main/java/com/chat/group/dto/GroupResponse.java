package com.chat.group.dto;

import com.chat.group.domain.UserGroup;

import java.time.OffsetDateTime;

public record GroupResponse(Long id, String name, boolean isDefault, OffsetDateTime createdAt) {

    public static GroupResponse from(UserGroup group) {
        return new GroupResponse(group.getId(), group.getName(), group.isDefault(), group.getCreatedAt());
    }
}
