package com.example.my_project_1.outbox.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "outbox_events")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private OutboxEventType eventType;

    @Lob
    private String payload;

    @Enumerated(EnumType.STRING)
    private OutboxStatus status;

    private int retryCount;
    private String lastError;

    private LocalDateTime createdAt;
    private LocalDateTime lastTriedAt;
    private LocalDateTime nextRetryAt;

    public static OutboxEvent create(OutboxEventType type, String payload) {
        OutboxEvent e = new OutboxEvent();
        e.eventType = type;
        e.payload = payload;
        e.status = OutboxStatus.PENDING;
        e.retryCount = 0;
        e.createdAt = LocalDateTime.now();
        e.nextRetryAt = LocalDateTime.now();
        return e;
    }

    public void markSuccess() {
        this.status = OutboxStatus.SUCCESS;
        this.lastTriedAt = LocalDateTime.now();
        this.nextRetryAt = null;
        this.lastError = null;
    }

    public void markFail(Exception e) {
        this.retryCount++;
        this.lastTriedAt = LocalDateTime.now();
        this.lastError = e.getMessage();

        long delay = Math.min(60, (long) Math.pow(2, retryCount));

        if (retryCount >= 5) {
            this.status = OutboxStatus.DEAD;
        } else {
            this.status = OutboxStatus.FAILED;
            this.nextRetryAt = LocalDateTime.now().plusSeconds(delay);
        }
    }

    public void markDead(String reason) {
        this.status = OutboxStatus.DEAD;
        this.lastError = reason;
        this.lastTriedAt = LocalDateTime.now();
    }

}
