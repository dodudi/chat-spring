package com.chat.common.batch;

import com.chat.invitation.infrastructure.InvitationRepository;
import com.chat.invitelink.infrastructure.InviteLinkRepository;
import com.chat.message.infrastructure.MessageRepository;
import com.chat.message.infrastructure.ReadCursorRepository;
import com.chat.room.infrastructure.ChatRoomMemberRepository;
import com.chat.room.infrastructure.ChatRoomRepository;
import com.chat.room.infrastructure.RoomGroupMembershipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmptyRoomCleanupJob {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final RoomGroupMembershipRepository roomGroupMembershipRepository;
    private final InvitationRepository invitationRepository;
    private final InviteLinkRepository inviteLinkRepository;
    private final MessageRepository messageRepository;
    private final ReadCursorRepository readCursorRepository;

    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void deleteEmptyRooms() {
        List<UUID> roomIds = chatRoomRepository.findEmptyRoomIds();
        if (roomIds.isEmpty()) {
            return;
        }

        readCursorRepository.deleteByRoomIdIn(roomIds);
        messageRepository.deleteByRoomIdIn(roomIds);
        inviteLinkRepository.deleteByRoomIdIn(roomIds);
        invitationRepository.deleteByRoomIdIn(roomIds);
        roomGroupMembershipRepository.deleteByRoomIdIn(roomIds);
        chatRoomMemberRepository.deleteByRoomIdIn(roomIds);
        chatRoomRepository.deleteByIdIn(roomIds);

        log.info("[BATCH_ROOM_CLEANUP] deleted={}", roomIds.size());
    }
}
