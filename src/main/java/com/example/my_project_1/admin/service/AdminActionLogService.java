package com.example.my_project_1.admin.service;

import com.example.my_project_1.admin.domain.AdminActionTargetType;
import com.example.my_project_1.admin.domain.AdminActionType;
import com.example.my_project_1.admin.service.response.AdminActionLogResponse;
import com.example.my_project_1.common.utils.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface AdminActionLogService {

    void log(
            Long adminId,
            AdminActionType actionType,
            AdminActionTargetType targetType,
            Long targetId,
            String description,
            Map<String, Object> metadata
    );

    PageResponse<AdminActionLogResponse> findLogs(
            AdminActionType actionType,
            AdminActionTargetType targetType,
            Long adminId,
            Pageable pageable
    );
}
