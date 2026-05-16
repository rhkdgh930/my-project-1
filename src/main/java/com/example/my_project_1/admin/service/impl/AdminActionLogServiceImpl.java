package com.example.my_project_1.admin.service.impl;

import com.example.my_project_1.admin.domain.AdminActionLog;
import com.example.my_project_1.admin.domain.AdminActionTargetType;
import com.example.my_project_1.admin.domain.AdminActionType;
import com.example.my_project_1.admin.repository.AdminActionLogRepository;
import com.example.my_project_1.admin.service.AdminActionLogService;
import com.example.my_project_1.admin.service.request.AdminActionLogSearchCondition;
import com.example.my_project_1.admin.service.response.AdminActionLogResponse;
import com.example.my_project_1.common.utils.DataSerializer;
import com.example.my_project_1.common.utils.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminActionLogServiceImpl implements AdminActionLogService {

    private final AdminActionLogRepository repository;
    private final Clock clock;

    @Override
    @Transactional
    public void log(
            Long adminId,
            AdminActionType actionType,
            AdminActionTargetType targetType,
            Long targetId,
            String description,
            Map<String, Object> metadata
    ) {
        AdminActionLog log = AdminActionLog.create(
                adminId,
                actionType,
                targetType,
                targetId,
                description,
                DataSerializer.serialize(metadata == null ? Map.of() : metadata),
                LocalDateTime.now(clock)
        );
        repository.save(log);
    }

    @Override
    public PageResponse<AdminActionLogResponse> findLogs(
            AdminActionType actionType,
            AdminActionTargetType targetType,
            Long adminId,
            Pageable pageable
    ) {
        AdminActionLogSearchCondition condition = new AdminActionLogSearchCondition(actionType, targetType, adminId);
        Page<AdminActionLogResponse> page = repository.searchLogs(condition, pageable)
                .map(AdminActionLogResponse::from);
        return PageResponse.of(page);
    }
}
