package com.chat.invitation.application;

import java.util.UUID;

public interface InvitationSender {

    void sendInvitation(String inviterId, UUID roomId, String inviteeId);
}
