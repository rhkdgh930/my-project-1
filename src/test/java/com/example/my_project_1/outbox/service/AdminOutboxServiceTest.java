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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
        OutboxEvent event = event(1L);
        event.markFail(new RuntimeException("temporary failure"), LocalDateTime.now(clock));
        when(outboxRepository.findById(1L)).thenReturn(Optional.of(event));

        service.retry(1L);

        assertResetForRetry(event);
        verify(outboxPublisher, never()).requestProcessing(1L);
    }

    @Test
    @DisplayName("DEAD 상태의 Outbox event는 admin retry를 허용한다.")
    void retry_allowsDeadEvent() {
        OutboxEvent event = event(1L);
        event.markDead("MAX_RETRY_EXCEEDED", LocalDateTime.now(clock));
        when(outboxRepository.findById(1L)).thenReturn(Optional.of(event));

        service.retry(1L);

        assertResetForRetry(event);
        verify(outboxPublisher, never()).requestProcessing(1L);
    }

    @Test
    @DisplayName("FAILED 상태의 Outbox event는 retry-now 성공 시 reset 후 즉시 처리를 요청한다.")
    void retryNow_allowsFailedEventAndRequestsProcessing() {
        OutboxEvent event = event(1L);
        event.markFail(new RuntimeException("temporary failure"), LocalDateTime.now(clock));
        when(outboxRepository.findById(1L)).thenReturn(Optional.of(event));

        service.retryNow(1L);

        assertResetForRetry(event);
        verify(outboxPublisher).requestProcessing(1L);
    }

    @Test
    @DisplayName("DEAD 상태의 Outbox event는 retry-now 성공 시 reset 후 즉시 처리를 요청한다.")
    void retryNow_allowsDeadEventAndRequestsProcessing() {
        OutboxEvent event = event(1L);
        event.markDead("MAX_RETRY_EXCEEDED", LocalDateTime.now(clock));
        when(outboxRepository.findById(1L)).thenReturn(Optional.of(event));

        service.retryNow(1L);

        assertResetForRetry(event);
        verify(outboxPublisher).requestProcessing(1L);
    }

    @Test
    @DisplayName("SUCCESS 상태의 Outbox event는 이미 성공 ErrorCode로 admin retry를 거부한다.")
    void retry_rejectsSuccessEvent() {
        OutboxEvent event = event(1L);
        event.markSuccess(LocalDateTime.now(clock));
        when(outboxRepository.findById(1L)).thenReturn(Optional.of(event));

        assertRetryRejected(event, ErrorCode.OUTBOX_ALREADY_SUCCEEDED);
        assertRetryNowRejected(event, ErrorCode.OUTBOX_ALREADY_SUCCEEDED);
    }

    @Test
    @DisplayName("PENDING 상태의 Outbox event는 이미 대기 중 ErrorCode로 admin retry를 거부한다.")
    void retry_rejectsPendingEvent() {
        OutboxEvent event = event(1L);
        when(outboxRepository.findById(1L)).thenReturn(Optional.of(event));

        assertRetryRejected(event, ErrorCode.OUTBOX_ALREADY_PENDING);
        assertRetryNowRejected(event, ErrorCode.OUTBOX_ALREADY_PENDING);
    }

    @Test
    @DisplayName("PROCESSING 상태의 Outbox event는 처리 중 ErrorCode로 admin retry를 거부한다.")
    void retry_rejectsProcessingEvent() {
        OutboxEvent event = event(1L);
        ReflectionTestUtils.setField(event, "status", OutboxStatus.PROCESSING);
        when(outboxRepository.findById(1L)).thenReturn(Optional.of(event));

        assertRetryRejected(event, ErrorCode.OUTBOX_ALREADY_PROCESSING);
        assertRetryNowRejected(event, ErrorCode.OUTBOX_ALREADY_PROCESSING);
    }

    @Test
    @DisplayName("없는 Outbox event id는 OUTBOX_EVENT_NOT_FOUND로 실패한다.")
    void retry_rejectsMissingEvent() {
        when(outboxRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.retry(1L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.OUTBOX_EVENT_NOT_FOUND);

        assertThatThrownBy(() -> service.retryNow(1L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.OUTBOX_EVENT_NOT_FOUND);

        verify(outboxPublisher, never()).requestProcessing(1L);
    }

    private void assertRetryRejected(OutboxEvent event, ErrorCode expectedErrorCode) {
        OutboxStatus before = event.getStatus();

        assertThatThrownBy(() -> service.retry(1L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(expectedErrorCode);

        assertThat(event.getStatus()).isEqualTo(before);
    }

    private void assertRetryNowRejected(OutboxEvent event, ErrorCode expectedErrorCode) {
        OutboxStatus before = event.getStatus();

        assertThatThrownBy(() -> service.retryNow(1L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(expectedErrorCode);

        assertThat(event.getStatus()).isEqualTo(before);
        verify(outboxPublisher, never()).requestProcessing(1L);
    }

    private void assertResetForRetry(OutboxEvent event) {
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(event.getRetryCount()).isZero();
        assertThat(event.getLastError()).isNull();
        assertThat(event.getLastTriedAt()).isNull();
        assertThat(event.getNextRetryAt()).isEqualTo(LocalDateTime.now(clock));
    }

    private OutboxEvent event(Long id) {
        OutboxEvent event = OutboxEvent.create(
                OutboxEventType.USER_ACCOUNT_CHANGED,
                "{}",
                "event-key",
                LocalDateTime.now(clock)
        );
        ReflectionTestUtils.setField(event, "id", id);
        return event;
    }
}
