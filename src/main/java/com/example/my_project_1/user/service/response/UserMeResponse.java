package com.example.my_project_1.user.service.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.example.my_project_1.user.domain.ProfileDetail;
import com.example.my_project_1.user.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Schema(description = "내 계정 조회 응답")
public class UserMeResponse {
    @Schema(description = "사용자 ID", example = "1")
    private Long id;

    @Schema(description = "이메일", example = "user@example.com")
    private String email;

    @Schema(description = "닉네임", example = "seoul_dev")
    private String nickname;

    @Schema(description = "권한", example = "USER")
    private String role;

    @Schema(description = "사용자 lifecycle 상태", example = "ACTIVE")
    private String userStatus;

    @Schema(description = "계정 상태", example = "NORMAL")
    private String accountStatus;

    @Schema(description = "자기소개", example = "백엔드 개발자입니다.", nullable = true)
    private String introduce;

    @Schema(description = "프로필 이미지 URL", example = "http://localhost:8080/images/default.png", nullable = true)
    private String profileImageUrl;

    @JsonProperty("isEmailVerified")
    @Schema(name = "isEmailVerified", description = "이메일 인증 여부", example = "true")
    private boolean emailVerified;

    @Schema(description = "마지막 로그인 시각", example = "2026-05-06T10:15:30", nullable = true)
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
