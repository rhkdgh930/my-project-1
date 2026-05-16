package com.example.my_project_1.admin.service.response;

import com.example.my_project_1.admin.domain.AdminActionLog;
import com.example.my_project_1.admin.domain.AdminActionTargetType;
import com.example.my_project_1.admin.domain.AdminActionType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "관리자 조치 감사 로그 응답")
public record AdminActionLogResponse(
        Long id,
        Long adminId,
        AdminActionType actionType,
        AdminActionTargetType targetType,
        Long targetId,
        String description,
        String metadata,
        LocalDateTime createdAt
) {
    public static AdminActionLogResponse from(AdminActionLog log) {
        return new AdminActionLogResponse(
                log.getId(),
                log.getAdminId(),
                log.getActionType(),
                log.getTargetType(),
                log.getTargetId(),
                log.getDescription(),
                log.getMetadata(),
                log.getCreatedAt()
        );
    }
}
