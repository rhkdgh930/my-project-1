package com.example.my_project_1.common.monitoring;

import com.example.my_project_1.admin.domain.AdminActionTargetType;
import com.example.my_project_1.admin.domain.AdminActionType;
import com.example.my_project_1.outbox.domain.OutboxEventType;
import com.example.my_project_1.report.domain.ReportTargetType;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MonitoringServiceTest {

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final MonitoringService monitoringService = new MonitoringService(meterRegistry);

    @Test
    @DisplayName("Outbox 처리 성공 counter를 eventType tag와 함께 증가시킨다.")
    void recordOutboxProcessSuccess_incrementsCounter() {
        monitoringService.recordOutboxProcessSuccess(OutboxEventType.USER_ACCOUNT_CHANGED);

        assertThat(meterRegistry.counter(
                MonitoringService.OUTBOX_PROCESS_SUCCESS,
                "eventType",
                OutboxEventType.USER_ACCOUNT_CHANGED.name()
        ).count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Post view sync 실패 counter를 reason tag와 함께 증가시킨다.")
    void recordPostViewSyncFail_incrementsCounter() {
        monitoringService.recordPostViewSyncFail("exception");

        assertThat(meterRegistry.counter(
                MonitoringService.POST_VIEW_SYNC_FAIL,
                "reason",
                "exception"
        ).count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("신고 생성 counter를 targetType tag와 함께 증가시킨다.")
    void recordReportCreated_incrementsCounter() {
        monitoringService.recordReportCreated(ReportTargetType.POST);

        assertThat(meterRegistry.counter(
                MonitoringService.REPORT_CREATED,
                "targetType",
                ReportTargetType.POST.name()
        ).count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("관리자 조치와 감사 로그 생성 counter를 낮은 cardinality tag로 증가시킨다.")
    void recordAdminMetrics_incrementCounters() {
        monitoringService.recordAdminModerationAction(AdminActionType.REPORT_DELETE_TARGET, ReportTargetType.POST.name());
        monitoringService.recordAdminAuditLogCreated(AdminActionType.REPORT_DELETE_TARGET, AdminActionTargetType.REPORT);

        assertThat(meterRegistry.counter(
                MonitoringService.ADMIN_MODERATION_ACTION,
                "actionType",
                AdminActionType.REPORT_DELETE_TARGET.name(),
                "targetType",
                ReportTargetType.POST.name()
        ).count()).isEqualTo(1.0);
        assertThat(meterRegistry.counter(
                MonitoringService.ADMIN_AUDIT_LOG_CREATED,
                "actionType",
                AdminActionType.REPORT_DELETE_TARGET.name(),
                "targetType",
                AdminActionTargetType.REPORT.name()
        ).count()).isEqualTo(1.0);
    }
}
