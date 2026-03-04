package com.example.my_project_1.auth.service.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class LoginRequestTest {
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

    @Test
    @DisplayName("loginRequestTest 생성을 성공한다.")
    void loginRequest_create_success_test() {
        //given
        LoginRequest request = LoginRequest.create("email@email.com", "password123*");

        //when
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        //then
        assertThat(violations).isEmpty();
        assertThat(request.getEmail()).isEqualTo("email@email.com");
        assertThat(request.getPassword()).isEqualTo("password123*");
    }

    @Test
    @DisplayName("이메일이 공백이면 검증에 실패한다.")
    void loginRequest_create_fail_test_email_blank() {
        // given
        LoginRequest request = LoginRequest.create(" ", "password");

        // when
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isNotEmpty();
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("이메일 입력은 필수입니다.");
    }

    @Test
    @DisplayName("비밀번호가 공백이면 검증에 실패한다.")
    void loginRequest_create_fail_test_password_blank() {
        // given
        LoginRequest request = LoginRequest.create("example@example.com", " ");

        // when
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isNotEmpty();
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("비밀번호 입력은 필수입니다.");
    }

    @Test
    @DisplayName("올바른 형식의 이메일이 아니면 검증에 실패한다.")
    void loginRequest_create_fail_test_email_wrong_type() {
        email_wrong_type_Test("example");
        email_wrong_type_Test("example@@");
        email_wrong_type_Test("example@@exmple.com");
        email_wrong_type_Test("example@example..com");
        email_wrong_type_Test("example@example.com.");
        email_wrong_type_Test("@example@example.com.");
    }

    private void email_wrong_type_Test(String wrongEmail) {
        // given
        LoginRequest request = LoginRequest.create(wrongEmail, "password");

        // when
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isNotEmpty();
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("올바른 형식의 이메일 주소여야 합니다.");
    }

    @Test
    @DisplayName("올바른 형식의 비밀번호가 아니면 검증에 실패한다.")
    void loginRequest_create_fail_test_password_wrong_type() {
        password_wrong_type("password123");
        password_wrong_type("password*");
        password_wrong_type("112152152*");
        password_wrong_type("         ");
    }

    private void password_wrong_type(String wrongPassword) {
        // given
        LoginRequest request = LoginRequest.create("email@email.com", wrongPassword);

        // when
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isNotEmpty();
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("비밀번호는 영문, 숫자, 특수문자를 포함하여 8자리 이상, 20자리 이하여야 합니다.");
    }

    @Test
    @DisplayName("비밀번호가 8자 미만이면 검증에 실패한다.")
    void loginRequest_create_fail_test_password_short_size() {
        // given
        LoginRequest request = LoginRequest.create("email@email.com", "short");

        // when
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isNotEmpty();
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("비밀번호는 영문, 숫자, 특수문자를 포함하여 8자리 이상, 20자리 이하여야 합니다.");

    }

    @Test
    @DisplayName("비밀번호가 21자 이상이면 검증에 실패한다.")
    void loginRequest_create_fail_test_password_long_size() {
        // given
        String longPassword = getLongPassword();
        LoginRequest request = LoginRequest.create("email@email.com", longPassword);

        // when
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isNotEmpty();
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("비밀번호는 영문, 숫자, 특수문자를 포함하여 8자리 이상, 20자리 이하여야 합니다.");

    }

    private static String getLongPassword() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 21; i++) {
            sb.append(i);
        }
        return sb.toString();
    }

}