package com.example.my_project_1.outbox.service;

import com.example.my_project_1.outbox.domain.OutboxEvent;
import com.example.my_project_1.outbox.domain.OutboxEventType;
import com.example.my_project_1.outbox.listener.OutboxSavedEvent;
import com.example.my_project_1.outbox.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Transactional
public class OutboxPublisher {

    private final OutboxRepository outboxRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

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
}