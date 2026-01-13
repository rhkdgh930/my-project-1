package com.example.my_project_1.user.service.response;

import com.example.my_project_1.user.domain.User;
import lombok.Getter;

@Getter
public class UserDetailResponse {
    private Long id;
    private String email;
    private String nickname;
    private String introduce;
    private String profileImageUrl;

    public static UserDetailResponse from(User user) {
        UserDetailResponse response = new UserDetailResponse();
        response.id = user.getId();
        response.email = user.getEmail().toString();
        response.nickname = user.getNickname();
        response.introduce = user.getProfileDetail().getIntroduce();
        response.profileImageUrl = user.getProfileDetail().getProfileImageUrl();
        return response;
    }
}
