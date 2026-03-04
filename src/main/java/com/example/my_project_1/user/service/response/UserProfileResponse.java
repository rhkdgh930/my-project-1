package com.example.my_project_1.user.service.response;

import com.example.my_project_1.user.domain.User;
import lombok.Getter;

@Getter
public class UserProfileResponse {
    private Long id;
    private String email;
    private String nickname;
    private String introduce;
    private String profileImageUrl;

    public static UserProfileResponse from(User user) {
        UserProfileResponse response = new UserProfileResponse();
        response.id = user.getId();
        response.email = user.getEmail().toString();
        response.nickname = user.getNickname();
        response.introduce = user.getProfileDetail().getIntroduce();
        response.profileImageUrl = user.getProfileDetail().getProfileImageUrl();
        return response;
    }
}
