package com.example.my_project_1.outbox.service.response;

import com.example.my_project_1.outbox.domain.OutboxEvent;
import com.example.my_project_1.outbox.domain.OutboxEventType;
import com.example.my_project_1.outbox.domain.OutboxStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Schema(description = "관리자 Outbox 이벤트 조회 응답. payload는 노출하지 않습니다.")
public class AdminOutboxResponse {
    private Long id;
    private OutboxEventType eventType;
    private String eventKey;
    private OutboxStatus status;
    private int retryCount;
    private LocalDateTime createdAt;
    private LocalDateTime lastTriedAt;
    private LocalDateTime nextRetryAt;
    private String lastError;

    public static AdminOutboxResponse from(OutboxEvent event) {
        AdminOutboxResponse response = new AdminOutboxResponse();
        response.id = event.getId();
        response.eventType = event.getEventType();
        response.eventKey = event.getEventKey();
        response.status = event.getStatus();
        response.retryCount = event.getRetryCount();
        response.createdAt = event.getCreatedAt();
        response.lastTriedAt = event.getLastTriedAt();
        response.nextRetryAt = event.getNextRetryAt();
        response.lastError = event.getLastError();
        return response;
    }
}
