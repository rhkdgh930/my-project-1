package com.example.my_project_1.outbox.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxEventTest {

    @Test
    @DisplayName("Outbox event 생성 시 PENDING 상태와 초기 retry 메타데이터를 설정한다.")
    void create_initializesPendingRetryMetadata() {
        LocalDateTime now = LocalDateTime.of(2026, 5, 8, 10, 0);

        OutboxEvent event = event(now);

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(event.getRetryCount()).isZero();
        assertThat(event.getCreatedAt()).isEqualTo(now);
        assertThat(event.getNextRetryAt()).isEqualTo(now);
        assertThat(event.getLastTriedAt()).isNull();
        assertThat(event.getLastError()).isNull();
    }

    @Test
    @DisplayName("markSuccess는 SUCCESS 상태로 바꾸고 retry 메타데이터를 정리한다.")
    void markSuccess_marksSuccessAndClearsRetryMetadata() {
        LocalDateTime now = LocalDateTime.of(2026, 5, 8, 10, 0);
        OutboxEvent event = event(now);
        event.markFail(new RuntimeException("temporary"), now.plusSeconds(1));

        event.markSuccess(now.plusSeconds(2));

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.SUCCESS);
        assertThat(event.getLastTriedAt()).isEqualTo(now.plusSeconds(2));
        assertThat(event.getNextRetryAt()).isNull();
        assertThat(event.getLastError()).isNull();
    }

    @Test
    @DisplayName("markFail은 FAILED 상태와 retry backoff 범위를 설정한다.")
    void markFail_marksFailedWithBackoffRange() {
        LocalDateTime now = LocalDateTime.of(2026, 5, 8, 10, 0);
        OutboxEvent event = event(now);

        event.markFail(new RuntimeException("temporary failure"), now);

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(event.getRetryCount()).isEqualTo(1);
        assertThat(event.getLastTriedAt()).isEqualTo(now);
        assertThat(event.getLastError()).isEqualTo("temporary failure");
        assertThat(event.getNextRetryAt())
                .isAfterOrEqualTo(now.plusSeconds(2))
                .isBeforeOrEqualTo(now.plusSeconds(6));
    }

    @Test
    @DisplayName("markFail이 최대 재시도 횟수에 도달하면 DEAD 상태가 된다.")
    void markFail_marksDeadWhenMaxRetryExceeded() {
        LocalDateTime now = LocalDateTime.of(2026, 5, 8, 10, 0);
        OutboxEvent event = event(now);

        for (int i = 0; i < 5; i++) {
            event.markFail(new RuntimeException("temporary failure"), now.plusSeconds(i));
        }

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.DEAD);
        assertThat(event.getRetryCount()).isEqualTo(5);
        assertThat(event.getNextRetryAt()).isNull();
        assertThat(event.getLastError()).startsWith("MAX_RETRY_EXCEEDED:");
    }

    @Test
    @DisplayName("lastError는 1000자 이하로 저장한다.")
    void markFail_truncatesLastError() {
        OutboxEvent event = event(LocalDateTime.now());
        String longMessage = "a".repeat(1500);

        event.markFail(new RuntimeException(longMessage), LocalDateTime.now());

        assertThat(event.getLastError()).hasSize(1000);
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.FAILED);
    }

    @Test
    @DisplayName("markProcessingTimeout은 FAILED 상태로 복구하고 즉시 재처리 가능하게 한다.")
    void markProcessingTimeout_marksFailedAndRetryNow() {
        LocalDateTime now = LocalDateTime.of(2026, 5, 8, 10, 0);
        OutboxEvent event = event(now);

        event.markProcessingTimeout(now.plusMinutes(5));

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(event.getRetryCount()).isEqualTo(1);
        assertThat(event.getLastTriedAt()).isEqualTo(now.plusMinutes(5));
        assertThat(event.getNextRetryAt()).isEqualTo(now.plusMinutes(5));
        assertThat(event.getLastError()).isEqualTo("PROCESSING_TIMEOUT");
    }

    @Test
    @DisplayName("markProcessingTimeout이 최대 재시도 횟수에 도달하면 DEAD 상태가 된다.")
    void markProcessingTimeout_marksDeadWhenMaxRetryExceeded() {
        LocalDateTime now = LocalDateTime.of(2026, 5, 8, 10, 0);
        OutboxEvent event = event(now);

        for (int i = 0; i < 5; i++) {
            event.markProcessingTimeout(now.plusMinutes(i));
        }

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.DEAD);
        assertThat(event.getRetryCount()).isEqualTo(5);
        assertThat(event.getNextRetryAt()).isNull();
        assertThat(event.getLastError()).isEqualTo("PROCESSING_TIMEOUT_MAX_RETRY_EXCEEDED");
    }

    @Test
    @DisplayName("markDead는 DEAD 상태와 사유를 기록한다.")
    void markDead_marksDeadAndTruncatesReason() {
        OutboxEvent event = event(LocalDateTime.now());
        String longReason = "a".repeat(1500);

        event.markDead(longReason, LocalDateTime.now());

        assertThat(event.getLastError()).hasSize(1000);
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.DEAD);
    }

    @Test
    @DisplayName("resetForRetry는 admin retry를 위해 PENDING 상태와 초기 retry 메타데이터로 되돌린다.")
    void resetForRetry_resetsRetryMetadata() {
        LocalDateTime now = LocalDateTime.of(2026, 5, 8, 10, 0);
        OutboxEvent event = event(now);
        event.markFail(new RuntimeException("temporary failure"), now.plusSeconds(1));

        event.resetForRetry(now.plusSeconds(2));

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(event.getRetryCount()).isZero();
        assertThat(event.getLastError()).isNull();
        assertThat(event.getLastTriedAt()).isNull();
        assertThat(event.getNextRetryAt()).isEqualTo(now.plusSeconds(2));
    }

    private OutboxEvent event(LocalDateTime now) {
        return OutboxEvent.create(
                OutboxEventType.USER_ACCOUNT_CHANGED,
                "{}",
                "event-key",
                now
        );
    }
}
