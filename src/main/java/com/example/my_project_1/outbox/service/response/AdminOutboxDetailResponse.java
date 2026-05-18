package com.example.my_project_1.outbox.service.response;

import com.example.my_project_1.outbox.domain.OutboxEvent;
import com.example.my_project_1.outbox.domain.OutboxEventType;
import com.example.my_project_1.outbox.domain.OutboxStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Schema(description = "관리자 Outbox 이벤트 상세 응답. ADMIN 전용이며 payload를 포함합니다.")
public class AdminOutboxDetailResponse {
    private Long id;
    private OutboxEventType eventType;
    private String eventKey;
    private OutboxStatus status;
    private String payload;
    private int retryCount;
    private LocalDateTime createdAt;
    private LocalDateTime lastTriedAt;
    private LocalDateTime nextRetryAt;
    private String lastError;

    public static AdminOutboxDetailResponse from(OutboxEvent event) {
        AdminOutboxDetailResponse response = new AdminOutboxDetailResponse();
        response.id = event.getId();
        response.eventType = event.getEventType();
        response.eventKey = event.getEventKey();
        response.status = event.getStatus();
        response.payload = event.getPayload();
        response.retryCount = event.getRetryCount();
        response.createdAt = event.getCreatedAt();
        response.lastTriedAt = event.getLastTriedAt();
        response.nextRetryAt = event.getNextRetryAt();
        response.lastError = event.getLastError();
        return response;
    }
}
