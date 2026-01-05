package com.example.my_project_1.user.domain;

import com.example.my_project_1.common.exception.CustomException;
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
    @DisplayName("닉네임 변경을 성공한다.")
    void updateNickname_success_test() {
        User user = getUser();
        assertThat(user.getNickname()).isEqualTo(NICKNAME);
        String changedNickname = "changedNickname";
        user.updateNickname(changedNickname);

        assertThat(user.getNickname()).isEqualTo(changedNickname);
    }

    @Test
    @DisplayName("비밀번호 변경을 성공한다.")
    void updatePassword_success_test() {
        User user = getUser();
        assertThat(user.getPassword()).isEqualTo(PASSWORD);
        String changedPassword = "changedPassword";
        user.updatePassword(changedPassword);

        assertThat(user.getPassword()).isEqualTo(changedPassword);
    }

    @Test
    @DisplayName("유저 삭제를 성공한다.")
    void delete_success_test() {
        User user = getUser();
        assertThat(user.isDeleted()).isFalse();

        user.delete();
        assertThat(user.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("유저 이메일 인증을 성공한다.")
    void verifyEmail_success_test() {
        User user = getUser();
        assertThat(user.isEmailVerified()).isFalse();
        assertThat(user.getUserStatus()).isEqualTo(UserStatus.PENDING);

        user.verifyEmail();
        assertThat(user.isEmailVerified()).isTrue();
        assertThat(user.getUserStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @DisplayName("유저 차단을 성공한다. ")
    @Test
    void suspend_success_test() {
        User user = getUser();
        assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.NORMAL);
        assertThat(user.isDeleted()).isFalse();

        user.suspend();

        assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.SUSPENDED);
        assertThat(user.isDeleted()).isFalse();
    }

    @DisplayName("삭제된 유저는 차단에 실패한다.")
    @Test
    void suspend_fail_test_already_deleted() {
        User user = getUser();
        user.delete();

        assertThat(user.isDeleted()).isTrue();
        assertThatThrownBy(() -> user.suspend())
                .isInstanceOf(CustomException.class)
                .hasMessage("존재하지 않는 사용자입니다.");
    }

    @DisplayName("이미 차단된 유저는 차단에 실패한다.")
    @Test
    void suspend_fail_test_already_suspended() {
        User user = getUser();
        user.suspend();

        assertThatThrownBy(() -> user.suspend())
                .isInstanceOf(CustomException.class)
                .hasMessage("차단된 계정입니다.");
    }

    @DisplayName("삭제되지 않고 이메일 인증을 완료한 유저는 활성화 상태입니다.")
    @Test
    void isActive_success_test() {
        User user = getUser();
        assertThat(user.getUserStatus()).isEqualTo(UserStatus.PENDING);
        assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.NORMAL);

        user.verifyEmail();
        assertThat(user.getUserStatus()).isEqualTo(UserStatus.ACTIVE);

        assertThat(user.isActive()).isTrue();
    }

    private static User getUser() {
        UserSignUpRequest request = createUserSignUpRequest();
        String encodedPassword = request.getPassword();
        return User.signUp(request, encodedPassword);
    }
}