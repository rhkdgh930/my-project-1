package com.example.my_project_1.outbox.service;

import com.example.my_project_1.outbox.domain.OutboxEvent;
import com.example.my_project_1.outbox.domain.OutboxEventType;
import com.example.my_project_1.outbox.handler.OutboxHandler;
import com.example.my_project_1.outbox.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OutboxProcessor {

    private final OutboxRepository outboxRepository;
    private final Map<OutboxEventType, OutboxHandler> handlerMap;

    public OutboxProcessor(OutboxRepository outboxRepository, List<OutboxHandler> handlers) {
        this.outboxRepository = outboxRepository;
        this.handlerMap = handlers.stream()
                .collect(Collectors.toMap(
                        OutboxHandler::getEventType,
                        h -> h));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(Long outboxId) {
        OutboxEvent event = outboxRepository.findById(outboxId)
                .orElseThrow();

        if (!event.canProcess()) return;

        try {
            OutboxHandler handler = handlerMap.get(event.getEventType());
            if (handler == null) {
                log.error("[OUTBOX][HANDLER_NOT_FOUND] type={}, id={}", event.getEventType(), outboxId);
                event.markDead("HANDLER_NOT_FOUND");
                return;
            }

            handler.handle(event.getPayload());

            event.markSuccess();

            log.debug("[OUTBOX][SUCCESS] type={}, id={}", event.getEventType(), outboxId);

        } catch (Exception e) {
            event.markFail(e);

            log.error("[OUTBOX][PROCESS_ERROR] type={}, id={}, retry={}, message={}",
                    event.getEventType(), outboxId, event.getRetryCount(), e.getMessage(), e);
        }

    }
}
