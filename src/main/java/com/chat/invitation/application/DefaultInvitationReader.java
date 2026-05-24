package com.chat.invitation.application;

import com.chat.invitation.domain.Invitation;
import com.chat.invitation.dto.InvitationResponse;
import com.chat.invitation.infrastructure.InvitationRepository;
import com.chat.room.domain.ChatRoom;
import com.chat.room.infrastructure.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DefaultInvitationReader implements InvitationReader {

    private final InvitationRepository invitationRepository;
    private final ChatRoomRepository chatRoomRepository;

    @Override
    public List<InvitationResponse> getPendingInvitations(String userId) {
        List<Invitation> invitations = invitationRepository.findPendingByInviteeId(userId);
        if (invitations.isEmpty()) {
            return List.of();
        }

        Set<UUID> roomIds = invitations.stream()
                .map(Invitation::getRoomId)
                .collect(Collectors.toSet());

        Map<UUID, ChatRoom> rooms = chatRoomRepository.findAllById(roomIds).stream()
                .collect(Collectors.toMap(ChatRoom::getId, r -> r));

        return invitations.stream()
                .filter(i -> rooms.containsKey(i.getRoomId()))
                .map(i -> InvitationResponse.of(i, rooms.get(i.getRoomId())))
                .toList();
    }
}
