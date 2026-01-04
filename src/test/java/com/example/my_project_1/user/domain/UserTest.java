package com.example.my_project_1.user.domain;

import com.example.my_project_1.user.service.request.UserSignUpRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.example.my_project_1.user.domain.UserFixture.*;
import static org.assertj.core.api.Assertions.*;

class UserTest {
    private final static String EMAIL = "email@email.com";
    private final static String PASSWORD = "password";
    private final static String NICKNAME = "nickname";

    @Test
    @DisplayName("유저 회원가입 성공 테스트")
    void signUpTest() {
        User user = getUser();

        assertThat(user.getEmail()).isEqualTo(EMAIL);
        assertThat(user.getPassword()).isEqualTo(PASSWORD);
        assertThat(user.getNickname()).isEqualTo(NICKNAME);
        assertThat(user.getRole()).isEqualTo(Role.USER);
        assertThat(user.getUserStatus()).isEqualTo(UserStatus.PENDING);
        assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.NORMAL);
        assertThat(user.isEmailVerified()).isFalse();
        assertThat(user.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("닉네임 변경 성공 테스트")
    void updateNicknameTest() {
        User user = getUser();
        assertThat(user.getNickname()).isEqualTo(NICKNAME);
        String changedNickname = "changedNickname";
        user.updateNickname(changedNickname);

        assertThat(user.getNickname()).isEqualTo(changedNickname);
    }

    @Test
    @DisplayName("비밀번호 변경 성공 테스트")
    void updatePasswordTest() {
        User user = getUser();
        assertThat(user.getPassword()).isEqualTo(PASSWORD);
        String changedPassword = "changedPassword";
        user.updatePassword(changedPassword);

        assertThat(user.getPassword()).isEqualTo(changedPassword);
    }

    @Test
    @DisplayName("유저 삭제 테스트")
    void withdrawTest() {
        User user = getUser();
        assertThat(user.isDeleted()).isFalse();

        user.delete();
        assertThat(user.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("유저 이메일 인증 테스트")
    void verifyEmailTest() {
        User user = getUser();
        assertThat(user.isEmailVerified()).isFalse();
        assertThat(user.getUserStatus()).isEqualTo(UserStatus.PENDING);

        user.verifyEmail();
        assertThat(user.isEmailVerified()).isTrue();
        assertThat(user.getUserStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    private static User getUser() {
        UserSignUpRequest request = createUserSignUpRequest();
        String encodedPassword = request.getPassword();
        return User.signUp(request, encodedPassword);
    }
}