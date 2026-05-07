package com.example.my_project_1.user.service.request;

import com.example.my_project_1.common.annotation.ValidPassword;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
@Schema(description = "회원가입 요청")
public class UserSignUpRequest {
    @Schema(description = "로그인 ID로 사용할 이메일", example = "user@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "이메일 입력은 필수입니다.")
    @Email(message = "올바른 형식의 이메일 주소여야 합니다.")
    private String email;

    @Schema(description = "비밀번호", example = "a12345678*", format = "password", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "비밀번호 입력은 필수입니다.")
    @ValidPassword
    private String password;

    @Schema(description = "닉네임. 4~20자입니다.", example = "seoul_dev", minLength = 4, maxLength = 20, requiredMode = Schema.RequiredMode.REQUIRED)
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
