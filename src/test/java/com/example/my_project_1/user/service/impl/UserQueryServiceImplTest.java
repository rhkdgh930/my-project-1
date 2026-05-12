package com.example.my_project_1.user.service.impl;

import com.example.my_project_1.common.exception.CustomException;
import com.example.my_project_1.common.exception.ErrorCode;
import com.example.my_project_1.user.domain.Email;
import com.example.my_project_1.user.domain.SuspensionReason;
import com.example.my_project_1.user.domain.SuspensionType;
import com.example.my_project_1.user.domain.User;
import com.example.my_project_1.user.repository.UserRepository;
import com.example.my_project_1.user.service.response.UserMeResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserQueryServiceImplTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserQueryServiceImpl userQueryService = new UserQueryServiceImpl(userRepository);

    @Test
    @DisplayName("내 정보 조회는 민감 정보를 제외한 현재 사용자 정보를 반환한다.")
    void getMe_returnsCurrentUserInfoWithoutSensitiveFields() {
        User user = activeUser();
        ReflectionTestUtils.setField(user, "id", 1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserMeResponse response = userQueryService.getMe(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("me@example.com");
        assertThat(response.getNickname()).isEqualTo("nickname");
        assertThat(response.getRole()).isEqualTo("USER");
        assertThat(response.getUserStatus()).isEqualTo("ACTIVE");
        assertThat(response.getAccountStatus()).isEqualTo("NORMAL");
        assertThat(response.getIntroduce()).isEqualTo("자기소개를 입력해주세요.");
        assertThat(response.getProfileImageUrl()).isNull();
        assertThat(response.isEmailVerified()).isTrue();
        assertThat(response.getLastLoginAt()).isEqualTo(LocalDateTime.parse("2026-01-01T00:00:00"));
        assertThat(Arrays.stream(UserMeResponse.class.getDeclaredFields()).map(Field::getName))
                .doesNotContain("password", "refreshToken", "socialId", "suspension", "withdrawal");
    }

    @Test
    @DisplayName("탈퇴 완료 사용자의 내 정보 조회는 거부한다.")
    void getMe_rejectsWithdrawnUser() {
        User user = activeUser();
        user.requestWithdrawal(LocalDateTime.parse("2026-01-01T00:00:00"));
        user.completeWithdrawal();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userQueryService.getMe(1L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("탈퇴 요청 상태 사용자의 내 정보 조회는 거부한다.")
    void getMe_rejectsWithdrawalRequestedUser() {
        User user = activeUser();
        user.requestWithdrawal(LocalDateTime.parse("2026-01-01T00:00:00"));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userQueryService.getMe(1L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.WITHDRAWAL_PENDING);
    }

    @Test
    @DisplayName("휴면 사용자의 내 정보 조회는 거부한다.")
    void getMe_rejectsDormantUser() {
        User user = activeUser();
        user.markDormant();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userQueryService.getMe(1L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_DORMANT);
    }

    @Test
    @DisplayName("차단 사용자의 내 정보 조회는 거부한다.")
    void getMe_rejectsSuspendedUser() {
        User user = activeUser();
        user.suspend(
                SuspensionType.TEMPORARY,
                SuspensionReason.OTHER,
                Duration.ofDays(1),
                LocalDateTime.parse("2026-01-01T00:00:00")
        );
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userQueryService.getMe(1L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_SUSPENDED);
    }

    private User activeUser() {
        return User.signUp(
                Email.from("me@example.com"),
                "encodedPassword",
                "nickname",
                LocalDateTime.parse("2026-01-01T00:00:00")
        );
    }
}
