package com.example.my_project_1.auth.service.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "로그인 요청")
public class LoginRequest {
    @Schema(description = "로그인 이메일", example = "user@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @Email(message = "올바른 형식의 이메일 주소여야 합니다.")
    @NotBlank(message = "이메일 입력은 필수입니다.")
    private String email;

    @Schema(description = "로그인 비밀번호", example = "a12345678*", format = "password", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "비밀번호 입력은 필수입니다.")
    private String password;

    public static LoginRequest create(String email, String password) {
        LoginRequest request = new LoginRequest();
        request.email = email;
        request.password = password;
        return request;
    }
}
