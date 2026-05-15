package com.example.my_project_1.report.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

import static org.springframework.util.Assert.hasText;
import static org.springframework.util.Assert.isTrue;
import static org.springframework.util.Assert.notNull;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "report",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_report_reporter_target",
                columnNames = {"reporter_id", "target_type", "target_id"}
        )
)
public class Report {

    private static final int MAX_REASON_LENGTH = 100;
    private static final int MAX_CONTENT_LENGTH = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private ReportTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(name = "reporter_id", nullable = false)
    private Long reporterId;

    @Column(nullable = false, length = 100)
    private String reason;

    @Column(nullable = false, length = 1000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportStatus status;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "reviewer_id")
    private Long reviewerId;

    public static Report create(
            ReportTargetType targetType,
            Long targetId,
            Long reporterId,
            String reason,
            String content
    ) {
        notNull(targetType, "targetType is required.");
        notNull(targetId, "targetId is required.");
        notNull(reporterId, "reporterId is required.");
        validateText(reason, MAX_REASON_LENGTH, "reason");
        validateText(content, MAX_CONTENT_LENGTH, "content");

        Report report = new Report();
        report.targetType = targetType;
        report.targetId = targetId;
        report.reporterId = reporterId;
        report.reason = reason;
        report.content = content;
        report.status = ReportStatus.PENDING;
        return report;
    }

    public void updateStatus(ReportStatus status, Long reviewerId, LocalDateTime now) {
        notNull(status, "status is required.");
        notNull(reviewerId, "reviewerId is required.");
        notNull(now, "now is required.");

        this.status = status;
        if (status == ReportStatus.PENDING) {
            this.reviewedAt = null;
            this.reviewerId = null;
            return;
        }
        this.reviewedAt = now;
        this.reviewerId = reviewerId;
    }

    private static void validateText(String value, int maxLength, String fieldName) {
        hasText(value, fieldName + " is required.");
        isTrue(value.length() <= maxLength, fieldName + " is too long.");
    }
}
