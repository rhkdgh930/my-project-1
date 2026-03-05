package com.example.my_project_1.user.service.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
public class UserRestoreRequest {
    @NotBlank(message = "본인 확인을 위해 비밀번호를 입력해주세요.")
    private String password;
}
