package com.example.my_project_1.report.service.request;

import com.example.my_project_1.report.domain.ReportStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "신고 상태 변경 요청")
public record ReportStatusUpdateRequest(
        @Schema(description = "변경할 신고 상태", example = "REVIEWED")
        @NotNull
        ReportStatus status
) {
}
