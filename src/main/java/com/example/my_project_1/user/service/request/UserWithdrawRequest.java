package com.example.my_project_1.user.service.request;

import com.example.my_project_1.common.annotation.ValidPassword;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class UserWithdrawRequest {
    @Schema(title = "비밀번호", description = "보안 정책에 따른 8자 이상 비밀번호",
            example = "a12345678*", format = "password", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "본인 확인을 위해 비밀번호를 입력해주세요.")
    @ValidPassword
    private String password;
}
