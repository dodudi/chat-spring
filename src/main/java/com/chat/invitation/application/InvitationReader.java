package com.chat.invitation.application;

import com.chat.invitation.dto.InvitationResponse;

import java.util.List;

public interface InvitationReader {

    List<InvitationResponse> getPendingInvitations(String userId);
}
