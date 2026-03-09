package com.example.my_project_1.user.service.request;

import com.example.my_project_1.common.annotation.ValidPassword;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
public class UserRestoreRequest {

    @Schema(title = "본인 확인 비밀번호", description = "계정 복구 승인을 위한 현재 비밀번호 입력",
            example = "a12345678*", format = "password", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "본인 확인을 위해 비밀번호를 입력해주세요.")
    @ValidPassword
    private String password;
}
