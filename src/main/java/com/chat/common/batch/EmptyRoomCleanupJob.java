package com.chat.common.batch;

import com.chat.room.infrastructure.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmptyRoomCleanupJob {

    private final ChatRoomRepository chatRoomRepository;

    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void deleteEmptyRooms() {
        int deleted = chatRoomRepository.deleteEmptyRooms();
        log.info("[BATCH_ROOM_CLEANUP] deleted={}", deleted);
    }
}
