package com.example.my_project_1.outbox.scheduler;

import com.example.my_project_1.outbox.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRecoveryScheduler {

    private final OutboxRepository outboxRepository;

    @Transactional
    @Scheduled(fixedDelay = 10_000)
    public void recover() {

        LocalDateTime threshold = LocalDateTime.now().minusMinutes(5);

        int recovered = outboxRepository.recoverStuckEvents(threshold);

        if (recovered > 0) {
            log.warn("[OUTBOX][RECOVER] recovered={}", recovered);
        }
    }
}