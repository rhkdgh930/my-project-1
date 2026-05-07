package com.example.my_project_1.user.service.response;

import com.example.my_project_1.user.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "프로필 수정 응답")
public class UserProfileResponse {
    @Schema(description = "사용자 ID", example = "1")
    private Long id;

    @Schema(description = "이메일", example = "user@example.com")
    private String email;

    @Schema(description = "닉네임", example = "seoul_dev")
    private String nickname;

    @Schema(description = "자기소개", example = "백엔드 개발자입니다.", nullable = true)
    private String introduce;

    @Schema(description = "프로필 이미지 URL", example = "http://localhost:8080/images/default.png", nullable = true)
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
