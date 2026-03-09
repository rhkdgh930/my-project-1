package com.example.my_project_1.user.service.request;

import com.example.my_project_1.common.annotation.ValidPassword;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class UserSignUpRequest {
    @Schema(title = "이메일 계정", description = "로그인 아이디로 사용될 유효한 이메일 주소",
            example = "gemini@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "이메일 입력은 필수입니다.")
    @Email
    private String email;

    @Schema(title = "비밀번호", description = "보안 정책에 따른 8자 이상 비밀번호",
            example = "a12345678*", format = "password", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "비밀번호 입력은 필수입니다.")
    @ValidPassword
    private String password;

    @Schema(title = "사용자 닉네임", description = "4~20자의 서비스 활동명",
            example = "seoul_dev", minLength = 4, maxLength = 20, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "닉네임은 필수입니다.")
    @Size(min = 4, max = 20)
    private String nickname;

    public static UserSignUpRequest create(String email, String password, String nickname) {
        UserSignUpRequest request = new UserSignUpRequest();
        request.email = email;
        request.password = password;
        request.nickname = nickname;
        return request;
    }
}
