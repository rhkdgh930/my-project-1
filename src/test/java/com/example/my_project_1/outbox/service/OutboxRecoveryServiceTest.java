package com.example.my_project_1.outbox.service;

import com.example.my_project_1.outbox.domain.OutboxEvent;
import com.example.my_project_1.outbox.domain.OutboxEventType;
import com.example.my_project_1.outbox.domain.OutboxStatus;
import com.example.my_project_1.outbox.repository.OutboxRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OutboxRecoveryServiceTest {

    private final Clock clock = Clock.fixed(
            Instant.parse("2026-05-08T10:00:00Z"),
            ZoneId.of("UTC")
    );
    private final OutboxRepository outboxRepository = mock(OutboxRepository.class);
    private final OutboxRecoveryService service = new OutboxRecoveryService(clock, outboxRepository);

    @Test
    @DisplayName("stuck PROCESSING 이벤트를 FAILED로 복구한다.")
    void recoverStuckEvents_marksProcessingEventFailed() {
        LocalDateTime now = LocalDateTime.now(clock);
        OutboxEvent event = processingEvent(1L, now.minusMinutes(10));
        when(outboxRepository.findStuckProcessingIds(any(), any(Pageable.class))).thenReturn(List.of(1L));
        when(outboxRepository.findById(1L)).thenReturn(Optional.of(event));

        int recovered = service.recoverStuckEvents();

        assertThat(recovered).isEqualTo(1);
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(event.getRetryCount()).isEqualTo(1);
        assertThat(event.getNextRetryAt()).isEqualTo(now);
        assertThat(event.getLastError()).isEqualTo("PROCESSING_TIMEOUT");
    }

    @Test
    @DisplayName("max retry에 도달한 stuck PROCESSING 이벤트는 DEAD로 복구한다.")
    void recoverStuckEvents_marksDeadWhenMaxRetryExceeded() {
        LocalDateTime now = LocalDateTime.now(clock);
        OutboxEvent event = processingEvent(1L, now.minusMinutes(10));
        ReflectionTestUtils.setField(event, "retryCount", 4);
        when(outboxRepository.findStuckProcessingIds(any(), any(Pageable.class))).thenReturn(List.of(1L));
        when(outboxRepository.findById(1L)).thenReturn(Optional.of(event));

        int recovered = service.recoverStuckEvents();

        assertThat(recovered).isEqualTo(1);
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.DEAD);
        assertThat(event.getRetryCount()).isEqualTo(5);
        assertThat(event.getNextRetryAt()).isNull();
    }

    @Test
    @DisplayName("stuck 이벤트가 없으면 0을 반환한다.")
    void recoverStuckEvents_returnsZeroWhenNoStuckEvents() {
        when(outboxRepository.findStuckProcessingIds(any(), any(Pageable.class))).thenReturn(List.of());

        int recovered = service.recoverStuckEvents();

        assertThat(recovered).isZero();
    }

    @Test
    @DisplayName("현재 시각 기준 5분 이전 PROCESSING 이벤트만 조회한다.")
    void recoverStuckEvents_usesFiveMinuteThreshold() {
        ArgumentCaptor<LocalDateTime> thresholdCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        when(outboxRepository.findStuckProcessingIds(thresholdCaptor.capture(), any(Pageable.class)))
                .thenReturn(List.of());

        service.recoverStuckEvents();

        assertThat(thresholdCaptor.getValue()).isEqualTo(LocalDateTime.now(clock).minusMinutes(5));
    }

    @Test
    @DisplayName("조회 후 이미 SUCCESS가 된 이벤트는 FAILED로 되돌리지 않는다.")
    void recoverStuckEvents_doesNotRegressNonProcessingEvent() {
        LocalDateTime now = LocalDateTime.now(clock);
        OutboxEvent event = processingEvent(1L, now.minusMinutes(10));
        event.markSuccess(now.minusSeconds(1));
        when(outboxRepository.findStuckProcessingIds(any(), any(Pageable.class))).thenReturn(List.of(1L));
        when(outboxRepository.findById(1L)).thenReturn(Optional.of(event));

        int recovered = service.recoverStuckEvents();

        assertThat(recovered).isZero();
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.SUCCESS);
    }

    private OutboxEvent processingEvent(Long id, LocalDateTime lastTriedAt) {
        OutboxEvent event = OutboxEvent.create(
                OutboxEventType.USER_ACCOUNT_CHANGED,
                "{}",
                "event-" + id,
                lastTriedAt.minusSeconds(1)
        );
        ReflectionTestUtils.setField(event, "id", id);
        ReflectionTestUtils.setField(event, "status", OutboxStatus.PROCESSING);
        ReflectionTestUtils.setField(event, "lastTriedAt", lastTriedAt);
        return event;
    }
}
