package com.example.my_project_1.outbox.service;

import com.example.my_project_1.common.monitoring.MonitoringService;
import com.example.my_project_1.outbox.domain.OutboxEventType;
import com.example.my_project_1.outbox.handler.OutboxHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OutboxProcessor {

    private final OutboxEventManager outboxEventManager;
    private final MonitoringService monitoringService;
    private final Map<OutboxEventType, OutboxHandler> handlerMap;

    public OutboxProcessor(
            OutboxEventManager outboxEventManager,
            MonitoringService monitoringService,
            List<OutboxHandler> handlers
    ) {
        this.outboxEventManager = outboxEventManager;
        this.monitoringService = monitoringService;
        this.handlerMap = handlers.stream()
                .collect(Collectors.toMap(OutboxHandler::getEventType, h -> h));
    }

    public void process(Long outboxId) {
        OutboxEventSnapshot snapshot = outboxEventManager.claim(outboxId);
        if (snapshot == null) {
            return;
        }

        try {
            OutboxHandler handler = handlerMap.get(snapshot.getEventType());

            if (handler == null) {
                outboxEventManager.markDead(snapshot.getId(), "HANDLER_NOT_FOUND");
                monitoringService.recordOutboxProcessFail(snapshot.getEventType());
                return;
            }

            handler.handle(snapshot.getPayload());
            outboxEventManager.markSuccess(snapshot.getId());
            monitoringService.recordOutboxProcessSuccess(snapshot.getEventType());

        } catch (Exception e) {
            outboxEventManager.markFail(snapshot.getId(), e);
            monitoringService.recordOutboxProcessFail(snapshot.getEventType());
            log.error("[OUTBOX][PROCESS_ERROR] id={} message={}", snapshot.getId(), e.getMessage(), e);
        }
    }
}
