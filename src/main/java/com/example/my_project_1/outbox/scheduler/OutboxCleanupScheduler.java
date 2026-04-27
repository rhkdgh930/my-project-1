package com.example.my_project_1.outbox.scheduler;

import com.example.my_project_1.outbox.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxCleanupScheduler {

    private final Clock clock;
    private final OutboxRepository outboxRepository;

    @Transactional
    @Scheduled(cron = "0 15 3 * * *")
    public void cleanupSuccessEvents() {
        LocalDateTime threshold = LocalDateTime.now(clock).minusDays(7);
        int deleted = outboxRepository.deleteSuccessBefore(threshold);

        if (deleted > 0) {
            log.info("[OUTBOX][CLEANUP] deleted={}", deleted);
        }
    }
}