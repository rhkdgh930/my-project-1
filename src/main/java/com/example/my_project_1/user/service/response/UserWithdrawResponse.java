package com.example.my_project_1.user.service.response;

import com.example.my_project_1.user.domain.User;
import lombok.Getter;

@Getter
public class UserWithdrawResponse {
    private Long id;
    private String email;
    private String nickname;
    private boolean deleted;

    public static UserWithdrawResponse from(User user) {
        UserWithdrawResponse response = new UserWithdrawResponse();
        response.id = user.getId();
        response.email = user.getEmail().getValue();
        response.nickname = user.getNickname();
        response.deleted = user.isDeleted();
        return response;
    }
}
