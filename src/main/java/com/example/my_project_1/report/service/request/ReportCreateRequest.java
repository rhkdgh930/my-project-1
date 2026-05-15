package com.example.my_project_1.report.service.request;

import com.example.my_project_1.report.domain.ReportTargetType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "신고 생성 요청")
public record ReportCreateRequest(
        @Schema(description = "신고 대상 타입", example = "POST")
        @NotNull
        ReportTargetType targetType,

        @Schema(description = "신고 대상 ID", example = "1")
        @NotNull
        Long targetId,

        @Schema(description = "신고 사유", example = "스팸")
        @NotBlank
        @Size(max = 100)
        String reason,

        @Schema(description = "신고 상세 내용", example = "동일한 광고성 내용이 반복됩니다.")
        @NotBlank
        @Size(max = 1000)
        String content
) {
}
