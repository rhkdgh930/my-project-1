package com.example.my_project_1.user.service.response;

import com.example.my_project_1.user.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Schema(description = "관리자용 유저 상세 응답")
public class UserDetailResponse {
    @Schema(description = "유저 ID", example = "1")
    private Long id;

    @Schema(description = "이메일", example = "user@example.com")
    private String email;

    @Schema(description = "닉네임", example = "seoul_dev")
    private String nickname;

    @Schema(description = "권한", example = "USER")
    private String role;

    @Schema(description = "유저 lifecycle 상태", example = "ACTIVE")
    private String userStatus;

    @Schema(description = "계정 상태", example = "NORMAL")
    private String accountStatus;

    @Schema(description = "이메일 인증 여부", example = "true")
    private boolean emailVerified;

    @Schema(description = "마지막 로그인 시각", example = "2026-05-06T10:00:00", nullable = true)
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
