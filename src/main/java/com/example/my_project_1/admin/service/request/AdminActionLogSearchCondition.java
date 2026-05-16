package com.example.my_project_1.admin.service.request;

import com.example.my_project_1.admin.domain.AdminActionTargetType;
import com.example.my_project_1.admin.domain.AdminActionType;

public record AdminActionLogSearchCondition(
        AdminActionType actionType,
        AdminActionTargetType targetType,
        Long adminId
) {
}
