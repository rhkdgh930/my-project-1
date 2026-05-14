package com.example.my_project_1.outbox.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

@Entity
@Getter
@Table(
        name = "outbox_events",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_outbox_event_key", columnNames = "event_key")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent {

    private static final int LAST_ERROR_MAX_LENGTH = 1000;
    private static final int MAX_RETRY_COUNT = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private OutboxEventType eventType;

    @Lob
    @Column(nullable = false)
    private String payload;

    @Column(name = "event_key", nullable = false)
    private String eventKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime lastTriedAt;

    private LocalDateTime nextRetryAt;

    @Column(length = LAST_ERROR_MAX_LENGTH)
    private String lastError;

    private int retryCount;

    public static OutboxEvent create(OutboxEventType type, String payload, String eventKey, LocalDateTime now) {
        return OutboxEvent.builder()
                .type(type)
                .payload(payload)
                .eventKey(eventKey)
                .now(now)
                .build();
    }

    public void markSuccess(LocalDateTime now) {
        this.status = OutboxStatus.SUCCESS;
        this.lastTriedAt = now;
        this.nextRetryAt = null;
        this.lastError = null;
    }

    public void markFail(Exception e, LocalDateTime now) {
        this.retryCount++;
        this.lastTriedAt = now;

        String message = resolveErrorMessage(e);

        if (retryCount >= MAX_RETRY_COUNT) {
            this.status = OutboxStatus.DEAD;
            this.nextRetryAt = null;
            this.lastError = truncate("MAX_RETRY_EXCEEDED: " + message);
            return;
        }

        long delay = Math.min(60, (long) Math.pow(2, retryCount));
        long jitter = ThreadLocalRandom.current().nextLong(0, 5);

        this.status = OutboxStatus.FAILED;
        this.nextRetryAt = now.plusSeconds(delay + jitter);
        this.lastError = truncate(message);
    }

    public void markProcessingTimeout(LocalDateTime now) {
        this.retryCount++;
        this.lastTriedAt = now;

        if (retryCount >= MAX_RETRY_COUNT) {
            this.status = OutboxStatus.DEAD;
            this.nextRetryAt = null;
            this.lastError = truncate("PROCESSING_TIMEOUT_MAX_RETRY_EXCEEDED");
            return;
        }

        this.status = OutboxStatus.FAILED;
        this.nextRetryAt = now;
        this.lastError = truncate("PROCESSING_TIMEOUT");
    }

    public void markDead(String reason, LocalDateTime now) {
        this.status = OutboxStatus.DEAD;
        this.lastError = truncate(reason);
        this.lastTriedAt = now;
        this.nextRetryAt = null;
    }

    public void resetForRetry(LocalDateTime now) {
        this.status = OutboxStatus.PENDING;
        this.nextRetryAt = now;
        this.retryCount = 0;
        this.lastError = null;
        this.lastTriedAt = null;
    }

    private String resolveErrorMessage(Exception e) {
        if (e == null) {
            return "UNKNOWN_ERROR";
        }

        return e.getMessage() != null
                ? e.getMessage()
                : e.getClass().getSimpleName();
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }

        return value.length() > LAST_ERROR_MAX_LENGTH
                ? value.substring(0, LAST_ERROR_MAX_LENGTH)
                : value;
    }

    @Builder
    private OutboxEvent(OutboxEventType type, String payload, String eventKey, LocalDateTime now) {
        Assert.notNull(type, "이벤트 타입은 필수입니다.");
        Assert.hasText(payload, "payload는 필수입니다.");
        Assert.hasText(eventKey, "이벤트 키는 필수입니다.");
        Assert.notNull(now, "시간 입력은 필수입니다.");

        this.eventType = type;
        this.payload = payload;
        this.eventKey = eventKey;
        this.status = OutboxStatus.PENDING;
        this.retryCount = 0;
        this.createdAt = now;
        this.nextRetryAt = now;
    }

}
