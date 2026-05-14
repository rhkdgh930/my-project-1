package com.example.my_project_1.outbox.service;

import com.example.my_project_1.outbox.domain.OutboxEvent;
import com.example.my_project_1.outbox.domain.OutboxEventType;
import com.example.my_project_1.outbox.listener.OutboxSavedEvent;
import com.example.my_project_1.outbox.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxRepository outboxRepository;
    private final OutboxEventInsertService outboxEventInsertService;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Transactional
    public void publish(OutboxEventType type, String payload, String eventKey) {
        OutboxEvent event = OutboxEvent.create(
                type,
                payload,
                eventKey,
                LocalDateTime.now(clock)
        );

        outboxRepository.save(event);
        eventPublisher.publishEvent(new OutboxSavedEvent(event.getId()));
    }

    public boolean publishIfAbsent(OutboxEventType type, String payload, String eventKey) {
        try {
            OutboxEvent event = OutboxEvent.create(
                    type,
                    payload,
                    eventKey,
                    LocalDateTime.now(clock)
            );

            OutboxEvent savedEvent = outboxEventInsertService.saveAndFlush(event);
            eventPublisher.publishEvent(new OutboxSavedEvent(savedEvent.getId()));
            return true;

        } catch (DataIntegrityViolationException e) {
            log.info("[OUTBOX][DUPLICATE_EVENT_KEY] type={} eventKey={}", type, eventKey);
            return false;
        }
    }


    public void requestProcessing(Long outboxId) {
        eventPublisher.publishEvent(new OutboxSavedEvent(outboxId));
    }
}
