package com.example.my_project_1.report.service.response;

import com.example.my_project_1.report.domain.Report;
import com.example.my_project_1.report.domain.ReportStatus;
import com.example.my_project_1.report.domain.ReportTargetType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "신고 응답")
public record ReportResponse(
        @Schema(description = "신고 ID", example = "1")
        Long id,

        @Schema(description = "신고 대상 타입", example = "POST")
        ReportTargetType targetType,

        @Schema(description = "신고 대상 ID", example = "10")
        Long targetId,

        @Schema(description = "신고자 ID", example = "3")
        Long reporterId,

        @Schema(description = "신고 사유", example = "스팸")
        String reason,

        @Schema(description = "신고 상세 내용")
        String content,

        @Schema(description = "신고 상태", example = "PENDING")
        ReportStatus status,

        @Schema(description = "신고 생성 시각")
        LocalDateTime createdAt,

        @Schema(description = "관리자 검토 시각")
        LocalDateTime reviewedAt,

        @Schema(description = "검토 관리자 ID", example = "1")
        Long reviewerId
) {

    public static ReportResponse from(Report report) {
        return new ReportResponse(
                report.getId(),
                report.getTargetType(),
                report.getTargetId(),
                report.getReporterId(),
                report.getReason(),
                report.getContent(),
                report.getStatus(),
                report.getCreatedAt(),
                report.getReviewedAt(),
                report.getReviewerId()
        );
    }
}
