package com.example.my_project_1.user.service.response;

import com.example.my_project_1.user.domain.AccountStatus;
import com.example.my_project_1.user.domain.User;
import com.example.my_project_1.user.domain.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "회원 탈퇴 요청 응답")
public class UserWithdrawResponse {
    @Schema(description = "사용자 ID", example = "1")
    private Long id;

    @Schema(description = "이메일", example = "user@example.com")
    private String email;

    @Schema(description = "닉네임", example = "seoul_dev")
    private String nickname;

    @Schema(description = "계정 상태", example = "NORMAL")
    private AccountStatus accountStatus;

    @Schema(description = "탈퇴 요청 후 사용자 상태", example = "WITHDRAWN_REQUESTED")
    private UserStatus userStatus;

    public static UserWithdrawResponse from(User user) {
        UserWithdrawResponse response = new UserWithdrawResponse();
        response.id = user.getId();
        response.email = user.getEmail().getValue();
        response.nickname = user.getNickname();
        response.accountStatus = user.getAccountStatus();
        response.userStatus = user.getUserStatus();
        return response;
    }
}
