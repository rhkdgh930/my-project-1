package com.example.my_project_1.user.service.request;

import com.example.my_project_1.common.annotation.ValidPassword;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;

@Getter
public class PasswordResetRequest {
    @Schema(title = "비밀번호 재설정 토큰", description = "이메일로 발송된 UUID 형식의 인증 토큰",
            example = "3c205e7b-c0db-41a0-95b7-2b99758725db", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "토큰은 필수입니다.")
    private String token;

    @Schema(title = "새 비밀번호", description = "8자 이상의 영문/숫자/특수문자 조합",
            example = "newpassword123*", format = "password", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "새 비밀번호는 필수입니다.")
    @ValidPassword
    private String newPassword;
}
