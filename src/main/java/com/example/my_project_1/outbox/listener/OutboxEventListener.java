package com.example.my_project_1.outbox.listener;

import com.example.my_project_1.outbox.service.OutboxProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventListener {

    private final OutboxProcessor outboxProcessor;

    @Async("asyncTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(OutboxSavedEvent event) {

        try {
            outboxProcessor.process(event.getOutboxId());
        } catch (Exception e) {
            log.error("[OUTBOX][ASYNC_PROCESS_FAIL] id={}", event.getOutboxId(), e);
        }
    }
}
