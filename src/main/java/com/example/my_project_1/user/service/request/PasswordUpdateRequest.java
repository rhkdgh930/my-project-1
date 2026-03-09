package com.example.my_project_1.user.service.request;

import com.example.my_project_1.common.annotation.ValidPassword;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;

@Getter
public class PasswordUpdateRequest {
    @Schema(title = "비밀번호", description = "보안 정책에 따른 8자 이상 비밀번호",
            example = "a12345678*", format = "password", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "현재 비밀번호는 필수입니다.")
    private String currentPassword;

    @Schema(title = "새 비밀번호", description = "8자 이상의 영문/숫자/특수문자 조합",
            example = "newpassword123*", format = "password", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "새 비밀번호는 필수입니다.")
    @ValidPassword
    private String newPassword;
}
