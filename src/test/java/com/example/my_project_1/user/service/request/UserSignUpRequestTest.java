package com.example.my_project_1.user.service.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UserSignUpRequestTest {
    /*
    EMAIL = "email@email.com";
    PASSWORD = "password";
    NICKNAME = "nickname";
     */
    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @DisplayName("userSignUpRequest 생성을 성공한다.")
    @Test
    void userSignUpRequest_create__success_test() {
        //given
        UserSignUpRequest request = UserSignUpRequest.create("email@email.com", "password", "nickname");

        //when
        Set<ConstraintViolation<UserSignUpRequest>> violations = validator.validate(request);

        //then
        assertThat(violations).isEmpty();
        assertThat(request.getEmail()).isEqualTo("email@email.com");
        assertThat(request.getPassword()).isEqualTo("password");
        assertThat(request.getNickname()).isEqualTo("nickname");
    }

    @Test
    @DisplayName("이메일이 공백이면 검증에 실패한다.")
    void userSignUpRequest_create_fail_test_email_blank() {
        // given
        UserSignUpRequest request = UserSignUpRequest.create(" ", "password", "nickname");

        // when
        Set<ConstraintViolation<UserSignUpRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isNotEmpty();
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("이메일 입력은 필수입니다.");
    }

    @Test
    @DisplayName("비밀번호가 공백이면 검증에 실패한다.")
    void userSignUpRequest_create_fail_test_password_blank() {
        // given
        UserSignUpRequest request = UserSignUpRequest.create("email@email.com", " ", "nickname");

        // when
        Set<ConstraintViolation<UserSignUpRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isNotEmpty();
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("비밀번호 입력은 필수입니다.");
    }

    @Test
    @DisplayName("닉네임이 공백이면 검증에 실패한다.")
    void userSignUpRequest_create_fail_test_nickname_blank() {
        // given
        UserSignUpRequest request = UserSignUpRequest.create("email@email.com", "password", " ");

        // when
        Set<ConstraintViolation<UserSignUpRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isNotEmpty();
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("닉네임은 필수입니다.");
    }

    @Test
    @DisplayName("올바른 형식의 이메일이 아니면 검증에 실패한다.")
    void userSignUpRequest_create_fail_test_email_wrong_type() {
        email_wrong_type_Test("example");
        email_wrong_type_Test("example@@");
        email_wrong_type_Test("example@@exmple.com");
        email_wrong_type_Test("example@example..com");
        email_wrong_type_Test("example@example.com.");
        email_wrong_type_Test("@example@example.com.");
    }

    private void email_wrong_type_Test(String wrongEmail) {
        // given
        UserSignUpRequest request = UserSignUpRequest.create(wrongEmail, "password", "nickname");

        // when
        Set<ConstraintViolation<UserSignUpRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isNotEmpty();
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("올바른 형식의 이메일 주소여야 합니다.");
    }

    @Test
    @DisplayName("비밀번호가 8자 미만이면 검증에 실패한다.")
    void userSignUpRequest_create_fail_test_password_short_size() {
        // given
        UserSignUpRequest request = UserSignUpRequest.create("email@email.com", "short", "nickname");

        // when
        Set<ConstraintViolation<UserSignUpRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isNotEmpty();
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("비밀번호는 8자리 이상 100자리 이하로 설정하셔야 합니다.");

    }

    @Test
    @DisplayName("비밀번호가 101자 이상이면 검증에 실패한다.")
    void userSignUpRequest_create_fail_test_password_long_size() {
        // given
        String longPassword = getLongPassword();
        UserSignUpRequest request = UserSignUpRequest.create("email@email.com", longPassword, "nickname");

        // when
        Set<ConstraintViolation<UserSignUpRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isNotEmpty();
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("비밀번호는 8자리 이상 100자리 이하로 설정하셔야 합니다.");

    }

    private static String getLongPassword() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 101; i++) {
            sb.append(i);
        }
        return sb.toString();
    }

}