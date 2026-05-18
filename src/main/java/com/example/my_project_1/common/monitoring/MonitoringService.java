package com.example.my_project_1.common.monitoring;

import com.example.my_project_1.admin.domain.AdminActionTargetType;
import com.example.my_project_1.admin.domain.AdminActionType;
import com.example.my_project_1.outbox.domain.OutboxEventType;
import com.example.my_project_1.report.domain.ReportTargetType;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonitoringService {

    public static final String OUTBOX_PROCESS_SUCCESS = "outbox.process.success";
    public static final String OUTBOX_PROCESS_FAIL = "outbox.process.fail";
    public static final String OUTBOX_RETRY_REQUEST = "outbox.retry.request";
    public static final String POST_VIEW_SYNC_SUCCESS = "post.view.sync.success";
    public static final String POST_VIEW_SYNC_FAIL = "post.view.sync.fail";
    public static final String POST_VIEW_SYNC_SKIPPED = "post.view.sync.skipped";
    public static final String REPORT_CREATED = "report.created";
    public static final String ADMIN_MODERATION_ACTION = "admin.moderation.action";
    public static final String ADMIN_AUDIT_LOG_CREATED = "admin.audit.log.created";

    private final MeterRegistry meterRegistry;

    public void recordOutboxProcessSuccess(OutboxEventType eventType) {
        increment(OUTBOX_PROCESS_SUCCESS, "eventType", eventType.name());
    }

    public void recordOutboxProcessFail(OutboxEventType eventType) {
        increment(OUTBOX_PROCESS_FAIL, "eventType", eventType.name());
    }

    public void recordOutboxRetryRequest(String mode) {
        increment(OUTBOX_RETRY_REQUEST, "mode", mode);
    }

    public void recordPostViewSyncSuccess() {
        increment(POST_VIEW_SYNC_SUCCESS);
    }

    public void recordPostViewSyncFail(String reason) {
        increment(POST_VIEW_SYNC_FAIL, "reason", reason);
    }

    public void recordPostViewSyncSkipped(String reason) {
        increment(POST_VIEW_SYNC_SKIPPED, "reason", reason);
    }

    public void recordReportCreated(ReportTargetType targetType) {
        increment(REPORT_CREATED, "targetType", targetType.name());
    }

    public void recordAdminModerationAction(AdminActionType actionType, String targetType) {
        increment(ADMIN_MODERATION_ACTION, "actionType", actionType.name(), "targetType", targetType);
    }

    public void recordAdminAuditLogCreated(AdminActionType actionType, AdminActionTargetType targetType) {
        increment(ADMIN_AUDIT_LOG_CREATED, "actionType", actionType.name(), "targetType", targetType.name());
    }

    private void increment(String name, String... tags) {
        try {
            meterRegistry.counter(name, tags).increment();
        } catch (Exception e) {
            log.warn("[MONITORING][METRIC_RECORD_FAIL] name={} message={}", name, e.getMessage());
        }
    }
}
