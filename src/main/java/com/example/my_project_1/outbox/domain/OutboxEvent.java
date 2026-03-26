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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private OutboxEventType eventType;

    @Lob
    private String payload;

    @Column(name = "event_key", nullable = false)
    private String eventKey;

    @Enumerated(EnumType.STRING)
    private OutboxStatus status;

    private int retryCount;
    private String lastError;

    private LocalDateTime createdAt;
    private LocalDateTime lastTriedAt;
    private LocalDateTime nextRetryAt;

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
        this.lastError = e.getMessage();

        long delay = Math.min(60, (long) Math.pow(2, retryCount));
        long jitter = ThreadLocalRandom.current().nextLong(0, 5);

        if (retryCount >= 5) {
            this.status = OutboxStatus.DEAD;
        } else {
            this.status = OutboxStatus.FAILED;
            this.nextRetryAt = now.plusSeconds(delay + jitter);
        }
    }

    public void markDead(String reason, LocalDateTime now) {
        this.status = OutboxStatus.DEAD;
        this.lastError = reason;
        this.lastTriedAt = now;
    }

    public void resetForRetry(LocalDateTime now) {
        this.status = OutboxStatus.PENDING;
        this.nextRetryAt = now;
        this.retryCount = 0;
        this.lastError = null;
    }

    @Builder
    private OutboxEvent(OutboxEventType type, String payload, String eventKey, LocalDateTime now) {
        Assert.notNull(type, "이벤트 타입은 필수입니다.");
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
