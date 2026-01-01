package com.example.my_project_1.user.service.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class UserSignUpRequest {
    @NotBlank(message = "이메일 입력은 필수입니다.")
    @Email(message = "올바른 형식의 이메일 주소여야 합니다.")
    private String email;

    @NotBlank(message = "비밀번호 입력은 필수입니다.")
    @Size(min = 8, max = 100)
    private String password;

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
