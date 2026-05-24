package com.chat.invitation.application;

import com.chat.invitation.dto.AcceptInvitationRequest;

public interface InvitationResponder {

    void accept(String userId, Long invitationId, AcceptInvitationRequest request);

    void reject(String userId, Long invitationId);
}
