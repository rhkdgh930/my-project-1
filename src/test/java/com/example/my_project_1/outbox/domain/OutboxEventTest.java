package com.example.my_project_1.outbox.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxEventTest {

    @Test
    @DisplayName("markFail은 lastError를 1000자 이하로 저장한다.")
    void markFail_truncatesLastError() {
        OutboxEvent event = OutboxEvent.create(
                OutboxEventType.USER_ACCOUNT_CHANGED,
                "{}",
                "event-key",
                LocalDateTime.now()
        );

        String longMessage = "a".repeat(1500);

        event.markFail(new RuntimeException(longMessage), LocalDateTime.now());

        assertThat(event.getLastError()).hasSize(1000);
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.FAILED);
    }

    @Test
    @DisplayName("markDead은 lastError를 1000자 이하로 저장한다.")
    void markDead_truncatesLastError() {
        OutboxEvent event = OutboxEvent.create(
                OutboxEventType.USER_ACCOUNT_CHANGED,
                "{}",
                "event-key",
                LocalDateTime.now()
        );

        String longReason = "a".repeat(1500);

        event.markDead(longReason, LocalDateTime.now());

        assertThat(event.getLastError()).hasSize(1000);
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.DEAD);
    }
}