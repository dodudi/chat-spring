package com.chat.common.batch;

import com.chat.message.infrastructure.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageRetentionJob {

    private static final int RETENTION_DAYS = 30;

    private final MessageRepository messageRepository;

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void deleteOldMessages() {
        OffsetDateTime cutoff = OffsetDateTime.now().minusDays(RETENTION_DAYS);
        int deleted = messageRepository.deleteOlderThan(cutoff);
        log.info("[BATCH_MSG_CLEANUP] deleted={} cutoff={}", deleted, cutoff);
    }
}
