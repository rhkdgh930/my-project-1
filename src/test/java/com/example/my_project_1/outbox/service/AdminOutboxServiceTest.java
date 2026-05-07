package com.example.my_project_1.outbox.service;

import com.example.my_project_1.common.exception.CustomException;
import com.example.my_project_1.common.exception.ErrorCode;
import com.example.my_project_1.outbox.domain.OutboxEvent;
import com.example.my_project_1.outbox.domain.OutboxEventType;
import com.example.my_project_1.outbox.domain.OutboxStatus;
import com.example.my_project_1.outbox.repository.OutboxRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminOutboxServiceTest {

    private final Clock clock = Clock.fixed(
            Instant.parse("2026-04-29T00:00:00Z"),
            ZoneId.of("UTC")
    );
    private final OutboxRepository outboxRepository = mock(OutboxRepository.class);
    private final OutboxPublisher outboxPublisher = mock(OutboxPublisher.class);
    private final AdminOutboxService service = new AdminOutboxService(clock, outboxRepository, outboxPublisher);

    @Test
    @DisplayName("FAILED 상태의 Outbox event는 admin retry를 허용한다.")
    void retry_allowsFailedEvent() {
        OutboxEvent event = event();
        event.markFail(new RuntimeException("temporary failure"), LocalDateTime.now(clock));
        when(outboxRepository.findById(1L)).thenReturn(Optional.of(event));

        service.retry(1L);

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(event.getRetryCount()).isZero();
        assertThat(event.getLastError()).isNull();
        assertThat(event.getLastTriedAt()).isNull();
        assertThat(event.getNextRetryAt()).isEqualTo(LocalDateTime.now(clock));
    }

    @Test
    @DisplayName("DEAD 상태의 Outbox event는 admin retry를 허용한다.")
    void retry_allowsDeadEvent() {
        OutboxEvent event = event();
        event.markDead("MAX_RETRY_EXCEEDED", LocalDateTime.now(clock));
        when(outboxRepository.findById(1L)).thenReturn(Optional.of(event));

        service.retry(1L);

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(event.getRetryCount()).isZero();
        assertThat(event.getLastError()).isNull();
        assertThat(event.getLastTriedAt()).isNull();
        assertThat(event.getNextRetryAt()).isEqualTo(LocalDateTime.now(clock));
    }

    @Test
    @DisplayName("SUCCESS 상태의 Outbox event는 admin retry를 거부한다.")
    void retry_rejectsSuccessEvent() {
        OutboxEvent event = event();
        event.markSuccess(LocalDateTime.now(clock));
        when(outboxRepository.findById(1L)).thenReturn(Optional.of(event));

        assertRetryNotAllowed(event);
    }

    @Test
    @DisplayName("PROCESSING 상태의 Outbox event는 admin retry를 거부한다.")
    void retry_rejectsProcessingEvent() {
        OutboxEvent event = event();
        ReflectionTestUtils.setField(event, "status", OutboxStatus.PROCESSING);
        when(outboxRepository.findById(1L)).thenReturn(Optional.of(event));

        assertRetryNotAllowed(event);
    }

    @Test
    @DisplayName("PENDING 상태의 Outbox event는 admin retry를 거부한다.")
    void retry_rejectsPendingEvent() {
        OutboxEvent event = event();
        when(outboxRepository.findById(1L)).thenReturn(Optional.of(event));

        assertRetryNotAllowed(event);
    }

    private void assertRetryNotAllowed(OutboxEvent event) {
        OutboxStatus before = event.getStatus();

        assertThatThrownBy(() -> service.retry(1L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.OUTBOX_RETRY_NOT_ALLOWED);

        assertThat(event.getStatus()).isEqualTo(before);
    }

    private OutboxEvent event() {
        return OutboxEvent.create(
                OutboxEventType.USER_ACCOUNT_CHANGED,
                "{}",
                "event-key",
                LocalDateTime.now(clock)
        );
    }
}