package com.example.my_project_1.user.service.response;

import com.example.my_project_1.user.domain.User;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserSignUpResponse {
    private Long id;
    private String email;
    private String nickname;

    public static UserSignUpResponse from(User user) {
        UserSignUpResponse response = new UserSignUpResponse();
        response.id = user.getId();
        response.email = user.getEmail();
        response.nickname = user.getNickname();
        return response;
    }
}

