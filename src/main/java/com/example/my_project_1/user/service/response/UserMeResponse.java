package com.example.my_project_1.user.service.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.example.my_project_1.user.domain.ProfileDetail;
import com.example.my_project_1.user.domain.User;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class UserMeResponse {
    private Long id;
    private String email;
    private String nickname;
    private String role;
    private String userStatus;
    private String accountStatus;
    private String introduce;
    private String profileImageUrl;
    @JsonProperty("isEmailVerified")
    private boolean emailVerified;
    private LocalDateTime lastLoginAt;

    public static UserMeResponse from(User user) {
        UserMeResponse response = new UserMeResponse();
        ProfileDetail profile = user.getProfileDetail();

        response.id = user.getId();
        response.email = user.getEmail().getValue();
        response.nickname = user.getNickname();
        response.role = user.getRole().name();
        response.userStatus = user.getUserStatus().name();
        response.accountStatus = user.getAccountStatus().name();
        response.introduce = profile.getIntroduce();
        response.profileImageUrl = profile.getProfileImageUrl();
        response.emailVerified = user.isEmailVerified();
        response.lastLoginAt = user.getLastLoginAt();
        return response;
    }
}
