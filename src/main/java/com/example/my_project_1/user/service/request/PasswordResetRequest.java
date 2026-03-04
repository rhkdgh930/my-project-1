package com.example.my_project_1.user.service.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;

@Getter
public class PasswordResetRequest {
    @NotBlank(message = "토큰은 필수입니다.")
    private String token;

    @NotBlank(message = "새 비밀번호는 필수입니다.")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,20}$",
            message = "비밀번호는 영문, 숫자, 특수문자를 포함하여 8자리 이상, 20자리 이하여야 합니다.")
    private String newPassword;
}
