package kr.it.rudy.chat.friend.dto;

import jakarta.validation.constraints.NotNull;

public record SendFriendRequestRequest(
        @NotNull Long receiverId
) {}
