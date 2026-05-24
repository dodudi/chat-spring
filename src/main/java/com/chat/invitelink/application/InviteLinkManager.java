package com.chat.invitelink.application;

import com.chat.invitelink.dto.CreateInviteLinkRequest;
import com.chat.invitelink.dto.InviteLinkResponse;

import java.util.UUID;

public interface InviteLinkManager {

    InviteLinkResponse createLink(String userId, UUID roomId, CreateInviteLinkRequest request);

    void deactivateLink(String userId, Long linkId);
}
