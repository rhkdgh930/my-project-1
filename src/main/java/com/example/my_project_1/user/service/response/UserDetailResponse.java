package com.example.my_project_1.user.service.response;

import com.example.my_project_1.user.domain.User;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class UserDetailResponse {
    private Long id;
    private String email;
    private String nickname;
    private String role;
    private String userStatus;
    private String accountStatus;
    private boolean emailVerified;
    private LocalDateTime lastLoginAt;

    public static UserDetailResponse from(User user) {
        UserDetailResponse response = new UserDetailResponse();
        response.id = user.getId();
        response.email = user.getEmail().getValue();
        response.nickname = user.getNickname();
        response.role = user.getRole().name();
        response.userStatus = user.getUserStatus().name();
        response.accountStatus = user.getAccountStatus().name();
        response.emailVerified = user.isEmailVerified();
        response.lastLoginAt = user.getLastLoginAt();
        return response;
    }
}
