package com.example.my_project_1.user.service.request;

import com.example.my_project_1.common.annotation.ValidPassword;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
@Schema(description = "비밀번호 재설정 요청")
public class PasswordResetRequest {
    @Schema(description = "이메일로 발송된 1회성 비밀번호 재설정 토큰", example = "3c205e7b-c0db-41a0-95b7-2b99758725db", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "토큰은 필수입니다.")
    private String token;

    @Schema(description = "새 비밀번호", example = "newpassword123*", format = "password", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "새 비밀번호는 필수입니다.")
    @ValidPassword
    private String newPassword;
}
