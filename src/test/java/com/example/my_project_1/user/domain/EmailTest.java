package com.example.my_project_1.user.domain;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

class EmailTest {

    @DisplayName("이메일 생성에 성공합니다.")
    @Test
    void email_create_success_test() {
        String value = "email@email.com";
        Email email = Email.from(value);
        assertThat(email.getValue()).isEqualTo(value);
    }

    @DisplayName("올바른 이메일 형식을 지키지 않으면 이메일 생성에 실패합니다.")
    @Test
    void email_create_fail_test_wrong_format() {
        email_create_fail_test_wrong_format("email");
        email_create_fail_test_wrong_format("email@");
        email_create_fail_test_wrong_format("email@emailcom");
        email_create_fail_test_wrong_format("email@email.");
        email_create_fail_test_wrong_format(".email@@email.com");
    }

    void email_create_fail_test_wrong_format(String value) {
        assertThatThrownBy(() -> Email.from(value))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("올바르지 않은 이메일 형식입니다.");
    }

    @Test
    void email_create_fail_test_email_is_null() {
        email_create_fail_test_email_is_null_or_empty(" ");
        email_create_fail_test_email_is_null_or_empty("");
        email_create_fail_test_email_is_null_or_empty(null);
    }

    private static void email_create_fail_test_email_is_null_or_empty(String value) {
        assertThatThrownBy(() -> Email.from(value))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이메일은 필수입니다.");
    }

}