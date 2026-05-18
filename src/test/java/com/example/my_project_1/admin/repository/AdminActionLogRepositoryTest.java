package com.example.my_project_1.admin.repository;

import com.example.my_project_1.admin.domain.AdminActionLog;
import com.example.my_project_1.admin.domain.AdminActionTargetType;
import com.example.my_project_1.admin.domain.AdminActionType;
import com.example.my_project_1.admin.service.request.AdminActionLogSearchCondition;
import com.example.my_project_1.common.config.QueryDslConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.datasource.url=jdbc:h2:mem:admin-action-log-repository-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver"
})
@Import(QueryDslConfig.class)
class AdminActionLogRepositoryTest {

    @Autowired
    private AdminActionLogRepository repository;

    @Test
    @DisplayName("필터가 없으면 생성 시각 내림차순과 id 내림차순으로 감사 로그를 조회한다.")
    void searchLogs_withoutFilters_returnsLatestLogs() {
        LocalDateTime now = LocalDateTime.of(2026, 5, 16, 10, 0);
        AdminActionLog older = log(1L, AdminActionType.USER_SUSPEND, AdminActionTargetType.USER, 10L, now.minusMinutes(1));
        AdminActionLog sameTimeLowId = log(1L, AdminActionType.OUTBOX_RETRY, AdminActionTargetType.OUTBOX, 20L, now);
        AdminActionLog sameTimeHighId = log(2L, AdminActionType.REPORT_STATUS_CHANGE, AdminActionTargetType.REPORT, 30L, now);
        repository.saveAllAndFlush(List.of(older, sameTimeLowId, sameTimeHighId));

        Page<AdminActionLog> page = repository.searchLogs(
                new AdminActionLogSearchCondition(null, null, null),
                PageRequest.of(0, 20)
        );

        assertThat(page.getContent()).extracting(AdminActionLog::getId)
                .containsExactly(sameTimeHighId.getId(), sameTimeLowId.getId(), older.getId());
    }

    @Test
    @DisplayName("actionType 필터로 감사 로그를 조회한다.")
    void searchLogs_filtersByActionType() {
        LocalDateTime now = LocalDateTime.of(2026, 5, 16, 10, 0);
        repository.saveAllAndFlush(List.of(
                log(1L, AdminActionType.USER_SUSPEND, AdminActionTargetType.USER, 10L, now),
                log(1L, AdminActionType.OUTBOX_RETRY, AdminActionTargetType.OUTBOX, 20L, now)
        ));

        Page<AdminActionLog> page = repository.searchLogs(
                new AdminActionLogSearchCondition(AdminActionType.USER_SUSPEND, null, null),
                PageRequest.of(0, 20)
        );

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getActionType()).isEqualTo(AdminActionType.USER_SUSPEND);
    }

    @Test
    @DisplayName("targetType 필터로 감사 로그를 조회한다.")
    void searchLogs_filtersByTargetType() {
        LocalDateTime now = LocalDateTime.of(2026, 5, 16, 10, 0);
        repository.saveAllAndFlush(List.of(
                log(1L, AdminActionType.USER_SUSPEND, AdminActionTargetType.USER, 10L, now),
                log(1L, AdminActionType.OUTBOX_RETRY, AdminActionTargetType.OUTBOX, 20L, now)
        ));

        Page<AdminActionLog> page = repository.searchLogs(
                new AdminActionLogSearchCondition(null, AdminActionTargetType.OUTBOX, null),
                PageRequest.of(0, 20)
        );

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getTargetType()).isEqualTo(AdminActionTargetType.OUTBOX);
    }

    @Test
    @DisplayName("adminId 필터로 감사 로그를 조회한다.")
    void searchLogs_filtersByAdminId() {
        LocalDateTime now = LocalDateTime.of(2026, 5, 16, 10, 0);
        repository.saveAllAndFlush(List.of(
                log(1L, AdminActionType.USER_SUSPEND, AdminActionTargetType.USER, 10L, now),
                log(2L, AdminActionType.USER_UNSUSPEND, AdminActionTargetType.USER, 20L, now)
        ));

        Page<AdminActionLog> page = repository.searchLogs(
                new AdminActionLogSearchCondition(null, null, 2L),
                PageRequest.of(0, 20)
        );

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getAdminId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("여러 필터 조합으로 감사 로그를 조회한다.")
    void searchLogs_filtersByCombinedConditions() {
        LocalDateTime now = LocalDateTime.of(2026, 5, 16, 10, 0);
        repository.saveAllAndFlush(List.of(
                log(1L, AdminActionType.USER_SUSPEND, AdminActionTargetType.USER, 10L, now),
                log(1L, AdminActionType.USER_SUSPEND, AdminActionTargetType.OUTBOX, 20L, now),
                log(2L, AdminActionType.USER_SUSPEND, AdminActionTargetType.USER, 30L, now)
        ));

        Page<AdminActionLog> page = repository.searchLogs(
                new AdminActionLogSearchCondition(AdminActionType.USER_SUSPEND, AdminActionTargetType.USER, 1L),
                PageRequest.of(0, 20)
        );

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getTargetId()).isEqualTo(10L);
    }

    private AdminActionLog log(
            Long adminId,
            AdminActionType actionType,
            AdminActionTargetType targetType,
            Long targetId,
            LocalDateTime createdAt
    ) {
        AdminActionLog log = AdminActionLog.create(
                adminId,
                actionType,
                targetType,
                targetId,
                "관리자 조치",
                "{}",
                createdAt
        );
        AdminActionLog saved = repository.saveAndFlush(log);
        ReflectionTestUtils.setField(saved, "createdAt", createdAt);
        return repository.saveAndFlush(saved);
    }
}
