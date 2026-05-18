package com.example.my_project_1.outbox.service;

import com.example.my_project_1.outbox.domain.OutboxEvent;
import com.example.my_project_1.outbox.domain.OutboxEventType;
import com.example.my_project_1.outbox.listener.OutboxSavedEvent;
import com.example.my_project_1.outbox.repository.OutboxRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OutboxPublisherTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-05-11T01:02:03Z"),
            ZoneId.of("Asia/Seoul")
    );

    private final OutboxRepository outboxRepository = mock(OutboxRepository.class);
    private final OutboxEventInsertService outboxEventInsertService = mock(OutboxEventInsertService.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final OutboxPublisher outboxPublisher = new OutboxPublisher(
            outboxRepository,
            outboxEventInsertService,
            eventPublisher,
            CLOCK
    );

    @Test
    @DisplayName("publish는 이벤트를 저장하고 OutboxSavedEvent를 발행한다.")
    void publish_savesEventAndPublishesSavedEvent() {
        outboxPublisher.publish(OutboxEventType.DORMANCY_NOTIFY, "{}", "event-key");

        ArgumentCaptor<OutboxEvent> eventCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxRepository).save(eventCaptor.capture());
        verify(eventPublisher).publishEvent(any(OutboxSavedEvent.class));

        OutboxEvent event = eventCaptor.getValue();
        assertThat(event.getEventType()).isEqualTo(OutboxEventType.DORMANCY_NOTIFY);
        assertThat(event.getPayload()).isEqualTo("{}");
        assertThat(event.getEventKey()).isEqualTo("event-key");
        assertThat(event.getCreatedAt()).isEqualTo(LocalDateTime.now(CLOCK));
    }

    @Test
    @DisplayName("publishIfAbsent는 저장에 성공하면 true를 반환하고 이벤트를 발행한다.")
    void publishIfAbsent_returnsTrueAndPublishesEventWhenSaveSucceeds() {
        when(outboxEventInsertService.saveAndFlush(any(OutboxEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        boolean published = outboxPublisher.publishIfAbsent(OutboxEventType.DORMANCY_NOTIFY, "{}", "event-key");

        assertThat(published).isTrue();
        verify(outboxEventInsertService).saveAndFlush(any(OutboxEvent.class));
        verify(eventPublisher).publishEvent(any(OutboxSavedEvent.class));
    }

    @Test
    @DisplayName("publishIfAbsent는 eventKey unique 충돌이면 false를 반환하고 이벤트를 발행하지 않는다.")
    void publishIfAbsent_returnsFalseWhenUniqueConstraintRaceHappens() {
        when(outboxEventInsertService.saveAndFlush(any(OutboxEvent.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        boolean published = outboxPublisher.publishIfAbsent(OutboxEventType.DORMANCY_NOTIFY, "{}", "event-key");

        assertThat(published).isFalse();
        verify(eventPublisher, never()).publishEvent(any());
    }
}
