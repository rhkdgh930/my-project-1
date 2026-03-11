package com.example.my_project_1.user.service.request;

import com.example.my_project_1.user.domain.SuspensionReason;
import com.example.my_project_1.user.domain.SuspensionType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.Duration;

@Getter
public class UserSuspensionRequest {
    @Schema(title = "차단 유형", description = "PERMANENT(영구차단) 또는 TEMPORARY(일시차단)",
            example = "TEMPORARY", requiredMode = Schema.RequiredMode.REQUIRED)
    private SuspensionType type;

    @Schema(title = "차단 사유", description = "운영 정책 위반 항목 (SPAM, ABUSE 등)",
            example = "SPAM", requiredMode = Schema.RequiredMode.REQUIRED)
    private SuspensionReason reason;

    @Schema(title = "차단 기간(일)", description = "일시차단(TEMPORARY)일 경우 필수 입력",
            example = "7", minimum = "1")
    private Long days;

    public Duration getDuration() {
        if (type == SuspensionType.PERMANENT) {
            return Duration.ZERO;
        }
        return Duration.ofDays(days != null ? days : 0);
    }
}
