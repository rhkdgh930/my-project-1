package com.example.my_project_1.user.service.request;

import com.example.my_project_1.common.exception.CustomException;
import com.example.my_project_1.common.exception.ErrorCode;
import com.example.my_project_1.user.domain.SuspensionReason;
import com.example.my_project_1.user.domain.SuspensionType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.Duration;

@Getter
@Schema(description = "관리자 유저 차단 요청")
public class UserSuspensionRequest {
    @Schema(title = "차단 유형", description = "TEMPORARY(일시 차단) 또는 PERMANENT(영구 차단)",
            example = "TEMPORARY", requiredMode = Schema.RequiredMode.REQUIRED)
    private SuspensionType type;

    @Schema(title = "차단 사유", description = "운영 정책 위반 항목입니다. SPAM, ABUSE, INAPPROPRIATE_CONTENT, FRAUD, OTHER 중 하나입니다.",
            example = "SPAM", requiredMode = Schema.RequiredMode.REQUIRED)
    private SuspensionReason reason;

    @Schema(title = "차단 기간(일)", description = "TEMPORARY일 경우 필요하며 1 이상이어야 합니다. PERMANENT일 경우 사용하지 않습니다.",
            example = "7", minimum = "1")
    private Long days;

    public Duration getDuration() {
        if (type == SuspensionType.PERMANENT) {
            return null;
        }

        if (days == null) {
            throw new CustomException(ErrorCode.INVALID_SUSPENSION_DURATION);
        }

        return Duration.ofDays(days);
    }
}
