package com.example.my_project_1.outbox.scheduler;

import com.example.my_project_1.outbox.service.OutboxRecoveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRecoveryScheduler {

    private final OutboxRecoveryService outboxRecoveryService;

    @Scheduled(fixedDelay = 10_000)
    public void recover() {
        int recovered = outboxRecoveryService.recoverStuckEvents();

        if (recovered > 0) {
            log.warn("[OUTBOX][RECOVER] recovered={}", recovered);
        }
    }
}