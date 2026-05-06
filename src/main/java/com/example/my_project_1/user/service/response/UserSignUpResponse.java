package com.example.my_project_1.user.service.response;

import com.example.my_project_1.user.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "회원가입 응답")
public class UserSignUpResponse {
    @Schema(description = "사용자 ID", example = "1")
    private Long id;

    @Schema(description = "가입 이메일", example = "user@example.com")
    private String email;

    @Schema(description = "닉네임", example = "seoul_dev")
    private String nickname;

    public static UserSignUpResponse from(User user) {
        UserSignUpResponse response = new UserSignUpResponse();
        response.id = user.getId();
        response.email = user.getEmail().getValue();
        response.nickname = user.getNickname();
        return response;
    }
}
