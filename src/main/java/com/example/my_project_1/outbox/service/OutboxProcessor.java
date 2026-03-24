package com.example.my_project_1.outbox.service;

import com.example.my_project_1.outbox.domain.OutboxEvent;
import com.example.my_project_1.outbox.domain.OutboxEventType;
import com.example.my_project_1.outbox.handler.OutboxHandler;
import com.example.my_project_1.outbox.repository.OutboxRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OutboxProcessor {

    private final Clock clock;
    private final OutboxRepository outboxRepository;
    private final Map<OutboxEventType, OutboxHandler> handlerMap;

    public OutboxProcessor(OutboxRepository outboxRepository, List<OutboxHandler> handlers, Clock clock) {
        this.clock = clock;
        this.outboxRepository = outboxRepository;
        this.handlerMap = handlers.stream()
                .collect(Collectors.toMap(
                        OutboxHandler::getEventType,
                        h -> h));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(Long outboxId) {
        LocalDateTime now = LocalDateTime.now(clock);

        //PENDING -> PROCESSING
        int updated = outboxRepository.claim(outboxId);
        if (updated == 0) {
            log.debug("[OUTBOX][SKIP_ALREADY_PROCESSING] id={}", outboxId);
            return;
        }

        OutboxEvent event = outboxRepository.findById(outboxId)
                .orElseThrow();

        try {
            OutboxHandler handler = handlerMap.get(event.getEventType());

            if (handler == null) {
                log.error("[OUTBOX][HANDLER_NOT_FOUND] type={}, id={}", event.getEventType(), outboxId);
                event.markDead("HANDLER_NOT_FOUND", now);
                return;
            }

            handler.handle(event.getPayload());
            //PROCESSING -> SUCCESS
            event.markSuccess(now);

            log.debug("[OUTBOX][SUCCESS] type={}, id={}", event.getEventType(), outboxId);

        } catch (Exception e) {
            //PROCESSING -> FAILED
            event.markFail(e, now);

            log.error("[OUTBOX][PROCESS_ERROR] type={}, id={}, retry={}, message={}",
                    event.getEventType(), outboxId, event.getRetryCount(), e.getMessage(), e);
        }

    }
}
