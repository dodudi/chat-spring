package com.chat.invitelink.application;

import com.chat.invitelink.dto.JoinByLinkRequest;

public interface InviteLinkJoiner {

    void joinByLink(String userId, String token, JoinByLinkRequest request);
}
