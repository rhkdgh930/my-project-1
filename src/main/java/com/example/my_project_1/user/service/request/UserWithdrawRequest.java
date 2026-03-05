package com.example.my_project_1.user.service.request;

import com.example.my_project_1.common.annotation.ValidPassword;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class UserWithdrawRequest {
    @NotBlank(message = "본인 확인을 위해 비밀번호를 입력해주세요.")
    @ValidPassword
    private String password;
}
