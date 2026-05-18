package com.example.my_project_1.admin.service.impl;

import com.example.my_project_1.admin.domain.AdminActionLog;
import com.example.my_project_1.admin.domain.AdminActionTargetType;
import com.example.my_project_1.admin.domain.AdminActionType;
import com.example.my_project_1.admin.repository.AdminActionLogRepository;
import com.example.my_project_1.admin.service.request.AdminActionLogSearchCondition;
import com.example.my_project_1.admin.service.response.AdminActionLogResponse;
import com.example.my_project_1.common.monitoring.MonitoringService;
import com.example.my_project_1.common.utils.PageResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminActionLogServiceImplTest {

    private final Clock clock = Clock.fixed(
            Instant.parse("2026-05-16T01:02:03Z"),
            ZoneId.of("Asia/Seoul")
    );
    private final AdminActionLogRepository repository = mock(AdminActionLogRepository.class);
    private final MonitoringService monitoringService = mock(MonitoringService.class);
    private final AdminActionLogServiceImpl service = new AdminActionLogServiceImpl(repository, monitoringService, clock);

    @Test
    @DisplayName("관리자 조치 감사 로그를 저장한다.")
    void log_savesAdminActionLog() {
        service.log(
                1L,
                AdminActionType.USER_SUSPEND,
                AdminActionTargetType.USER,
                2L,
                "관리자가 유저를 정지했습니다.",
                Map.of("reason", "SPAM")
        );

        verify(repository).save(any(AdminActionLog.class));
        verify(monitoringService).recordAdminAuditLogCreated(AdminActionType.USER_SUSPEND, AdminActionTargetType.USER);
    }

    @Test
    @DisplayName("관리자 조치 감사 로그 목록을 필터 조건으로 조회한다.")
    void findLogs_returnsFilteredPage() {
        PageRequest pageable = PageRequest.of(0, 20);
        AdminActionLog log = AdminActionLog.create(
                1L,
                AdminActionType.USER_SUSPEND,
                AdminActionTargetType.USER,
                2L,
                "관리자가 유저를 정지했습니다.",
                "{\"reason\":\"SPAM\"}",
                LocalDateTime.now(clock)
        );
        when(repository.searchLogs(any(AdminActionLogSearchCondition.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(log), pageable, 1));

        PageResponse<AdminActionLogResponse> response = service.findLogs(
                AdminActionType.USER_SUSPEND,
                AdminActionTargetType.USER,
                1L,
                pageable
        );

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).actionType()).isEqualTo(AdminActionType.USER_SUSPEND);
        assertThat(response.getContent().get(0).targetType()).isEqualTo(AdminActionTargetType.USER);
    }
}
