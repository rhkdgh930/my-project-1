package com.example.my_project_1.outbox.service;

import com.example.my_project_1.admin.domain.AdminActionTargetType;
import com.example.my_project_1.admin.domain.AdminActionType;
import com.example.my_project_1.admin.service.AdminActionLogService;
import com.example.my_project_1.common.exception.CustomException;
import com.example.my_project_1.common.exception.ErrorCode;
import com.example.my_project_1.common.utils.PageResponse;
import com.example.my_project_1.outbox.domain.OutboxEvent;
import com.example.my_project_1.outbox.domain.OutboxEventType;
import com.example.my_project_1.outbox.domain.OutboxStatus;
import com.example.my_project_1.outbox.repository.OutboxRepository;
import com.example.my_project_1.outbox.service.response.AdminOutboxDetailResponse;
import com.example.my_project_1.outbox.service.response.AdminOutboxResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
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
    private final AdminActionLogService adminActionLogService = mock(AdminActionLogService.class);
    private final AdminOutboxService service = new AdminOutboxService(clock, outboxRepository, outboxPublisher, adminActionLogService);

    @Test
    @DisplayName("status 필터가 없으면 전체 Outbox event page를 payload 없이 조회한다.")
    void findPage_returnsAllEventsWithoutPayload() {
        PageRequest pageable = PageRequest.of(0, 20);
        OutboxEvent event = event(1L);
        when(outboxRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(event), pageable, 1));

        PageResponse<AdminOutboxResponse> response = service.findPage(null, pageable);

        assertThat(response.getContent()).hasSize(1);
        AdminOutboxResponse item = response.getContent().get(0);
        assertThat(item.getId()).isEqualTo(1L);
        assertThat(item.getEventType()).isEqualTo(OutboxEventType.USER_ACCOUNT_CHANGED);
        assertThat(item.getEventKey()).isEqualTo("event-key");
        assertThat(item.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(item.getRetryCount()).isZero();
        assertThat(item.getCreatedAt()).isEqualTo(LocalDateTime.now(clock));
        assertThat(item.getLastTriedAt()).isNull();
        assertThat(item.getNextRetryAt()).isEqualTo(LocalDateTime.now(clock));
        assertThat(item.getLastError()).isNull();
    }

    @Test
    @DisplayName("status 필터가 있으면 해당 상태의 Outbox event page만 조회한다.")
    void findPage_filtersByStatus() {
        PageRequest pageable = PageRequest.of(0, 20);
        OutboxEvent event = event(1L);
        event.markFail(new RuntimeException("temporary failure"), LocalDateTime.now(clock));
        when(outboxRepository.findAllByStatus(OutboxStatus.FAILED, pageable))
                .thenReturn(new PageImpl<>(List.of(event), pageable, 1));

        PageResponse<AdminOutboxResponse> response = service.findPage(OutboxStatus.FAILED, pageable);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(response.getContent().get(0).getLastError()).isEqualTo("temporary failure");
        verify(outboxRepository, never()).findAll(pageable);
    }

    @Test
    @DisplayName("Outbox event 상세 조회는 payload를 포함한다.")
    void findById_returnsDetailWithPayload() {
        OutboxEvent event = event(1L);
        when(outboxRepository.findById(1L)).thenReturn(Optional.of(event));

        AdminOutboxDetailResponse response = service.findById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEventType()).isEqualTo(OutboxEventType.USER_ACCOUNT_CHANGED);
        assertThat(response.getEventKey()).isEqualTo("event-key");
        assertThat(response.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(response.getPayload()).isEqualTo("{\"userId\":1}");
        assertThat(response.getRetryCount()).isZero();
        assertThat(response.getCreatedAt()).isEqualTo(LocalDateTime.now(clock));
        assertThat(response.getLastTriedAt()).isNull();
        assertThat(response.getNextRetryAt()).isEqualTo(LocalDateTime.now(clock));
        assertThat(response.getLastError()).isNull();
    }

    @Test
    @DisplayName("없는 Outbox event id 상세 조회는 OUTBOX_EVENT_NOT_FOUND로 실패한다.")
    void findById_rejectsMissingEvent() {
        when(outboxRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(1L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.OUTBOX_EVENT_NOT_FOUND);
    }

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
    @DisplayName("Outbox event admin retry 성공 시 감사 로그를 저장한다.")
    void retryByAdmin_writesAuditLog() {
        OutboxEvent event = event(1L);
        event.markFail(new RuntimeException("temporary failure"), LocalDateTime.now(clock));
        when(outboxRepository.findById(1L)).thenReturn(Optional.of(event));

        service.retry(1L, 99L);

        verify(adminActionLogService).log(
                99L,
                AdminActionType.OUTBOX_RETRY,
                AdminActionTargetType.OUTBOX,
                1L,
                "관리자가 Outbox 이벤트 재시도를 예약했습니다.",
                Map.of()
        );
    }

    @Test
    @DisplayName("Outbox event retry-now 성공 시 감사 로그를 저장한다.")
    void retryNowByAdmin_writesAuditLog() {
        OutboxEvent event = event(1L);
        event.markFail(new RuntimeException("temporary failure"), LocalDateTime.now(clock));
        when(outboxRepository.findById(1L)).thenReturn(Optional.of(event));

        service.retryNow(1L, 99L);

        verify(adminActionLogService).log(
                99L,
                AdminActionType.OUTBOX_RETRY_NOW,
                AdminActionTargetType.OUTBOX,
                1L,
                "관리자가 Outbox 이벤트 즉시 재시도를 요청했습니다.",
                Map.of()
        );
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
                "{\"userId\":1}",
                "event-key",
                LocalDateTime.now(clock)
        );
        ReflectionTestUtils.setField(event, "id", id);
        return event;
    }
}
