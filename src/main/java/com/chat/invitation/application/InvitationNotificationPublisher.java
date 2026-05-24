package com.chat.invitation.application;

import com.chat.invitation.dto.InvitationResponse;

public interface InvitationNotificationPublisher {

    void publish(String userId, InvitationResponse invitation);
}
