package com.example.my_project_1.user.domain;

import com.example.my_project_1.common.exception.CustomException;
import com.example.my_project_1.user.service.request.UserSignUpRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;

import static com.example.my_project_1.user.domain.UserFixture.*;
import static org.assertj.core.api.Assertions.*;

class UserTest {
    private final static String EMAIL = "email@email.com";
    private final static String PASSWORD = "password123*";
    private final static String NICKNAME = "nickname";
    private static final String DEFAULT_INTRODUCE = "자기소개를 입력해주세요.";
    private static final String DEFAULT_IMG_URL = "uploads/default.png";

    @Test
    @DisplayName("유저 회원가입 성공 테스트")
    void signUp_success_Test() {
        User user = getVerifiedUser();

        assertThat(user.getEmail().getValue()).isEqualTo(EMAIL);
        assertThat(user.getPassword()).isEqualTo(PASSWORD);
        assertThat(user.getNickname()).isEqualTo(NICKNAME);
        assertThat(user.getRole()).isEqualTo(Role.USER);
        assertThat(user.getUserStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.NORMAL);
        assertThat(user.isEmailVerified()).isTrue();
        assertThat(user.isDeleted()).isFalse();

        assertThat(user.getProfileDetail().getIntroduce()).isEqualTo(DEFAULT_INTRODUCE);
        assertThat(user.getProfileDetail().getProfileImageUrl()).isEqualTo(DEFAULT_IMG_URL);
    }

    @DisplayName("필드에 null값이 들어가면 회원가입을 실패합니다.")
    @Test
    void signUp_fail_test() {
        signUp_fail_test(null, PASSWORD, NICKNAME, "이메일은 필수입니다.");
        signUp_fail_test(EMAIL, null, NICKNAME, "비밀번호는 필수입니다.");
        signUp_fail_test(EMAIL, PASSWORD, null, "닉네임은 필수입니다.");
    }

    void signUp_fail_test(String email, String password, String nickname, String message) {
        assertThatThrownBy(() -> User.signUp(Email.from(email), password, nickname, LocalDateTime.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(message);
    }

    @Test
    @DisplayName("닉네임 변경을 성공한다.")
    void updateNickname_success_test() {
        User user = getVerifiedUser();

        assertThat(user.getNickname()).isEqualTo(NICKNAME);
        String changedNickname = "changedNickname";
        user.updateNickname(changedNickname);

        assertThat(user.getNickname()).isEqualTo(changedNickname);
    }

    @DisplayName("닉네임이 빈칸이나 널값일 경우 닉네임 변경을 실패합니다.")
    @Test
    void updateNickname_fail_test_text() {

        User unverifiedUser = getVerifiedUser();

        updateNickname_fail_test_text(unverifiedUser, " ");
        updateNickname_fail_test_text(unverifiedUser, "");
    }

    private void updateNickname_fail_test_text(User unverifiedUser, String newNickname) {
        assertThatThrownBy(() -> unverifiedUser.updateNickname(newNickname))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("닉네임은 필수입니다.");
    }

    @Test
    @DisplayName("비밀번호 변경을 성공한다.")
    void updatePassword_success_test() {
        User user = getVerifiedUser();

        assertThat(user.getPassword()).isEqualTo(PASSWORD);
        String changedPassword = "changedPassword";
        user.updatePassword(changedPassword);

        assertThat(user.getPassword()).isEqualTo(changedPassword);
    }

    @DisplayName("비밀번호가 빈칸이나 널값일 경우 비밀번호 변경을 실패합니다.")
    @Test
    void updatePassword_fail_test_text() {
        User unverifiedUser = getVerifiedUser();

        updatePassword_fail_test_text(unverifiedUser, " ");
        updatePassword_fail_test_text(unverifiedUser, "");
    }

    private static void updatePassword_fail_test_text(User unverifiedUser, String newPassword) {
        assertThatThrownBy(() -> unverifiedUser.updatePassword(newPassword))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("비밀번호는 필수입니다.");
    }

    @Test
    @DisplayName("유저 삭제를 성공한다.")
    void delete_success_test() {
        User user = getVerifiedUser();
        assertThat(user.isDeleted()).isFalse();

        user.softDelete(LocalDateTime.now());
        assertThat(user.isDeleted()).isTrue();
    }

    @DisplayName("유저 차단을 성공한다. ")
    @Test
    void suspend_success_test() {
        User user = getVerifiedUser();
        assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.NORMAL);
        assertThat(user.isDeleted()).isFalse();

        user.suspend(SuspensionType.PERMANENT, SuspensionReason.OTHER, Duration.ofDays(1), LocalDateTime.now());

        assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.SUSPENDED);
        assertThat(user.isDeleted()).isFalse();
    }

    @DisplayName("삭제된 유저는 차단에 실패한다.")
    @Test
    void suspend_fail_test_already_deleted() {
        User user = getVerifiedUser();
        user.softDelete(LocalDateTime.now());

        assertThat(user.isDeleted()).isTrue();
        assertThatThrownBy(() -> user.suspend(SuspensionType.PERMANENT, SuspensionReason.OTHER, Duration.ofDays(1), LocalDateTime.now()))
                .isInstanceOf(CustomException.class)
                .hasMessage("존재하지 않는 사용자입니다.");
    }

    @DisplayName("삭제되지 않고 이메일 인증을 완료한 유저는 활성화 상태입니다.")
    @Test
    void isActive_success_test() {
        User user = getVerifiedUser();
        assertThat(user.getUserStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.NORMAL);

        assertThat(user.getUserStatus()).isEqualTo(UserStatus.ACTIVE);

        assertThat(user.isActive()).isTrue();
    }

    private static User getVerifiedUser() {
        UserSignUpRequest request = createUserSignUpRequest();
        String encodedPassword = request.getPassword();
        return User.signUp(
                Email.from(request.getEmail()),
                encodedPassword,
                request.getNickname(),
                LocalDateTime.now());
    }
}