package com.example.my_project_1.user.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.example.my_project_1.user.domain.UserFixture.*;
import static org.assertj.core.api.Assertions.*;

class UserTest {
    User user;
    PasswordEncoder passwordEncoder;

//    EMAIL = "email@email.com";
//    PASSWORD = "password";
//    NICKNAME = "nickname";


    @BeforeEach
    void setUp() {
        this.passwordEncoder = createPasswordEncoder();
        user = User.signUp(createUserSignUpRequest(), passwordEncoder);
    }

    @Test
    void signUpTest() {
        assertThat(user.getEmail()).isEqualTo("email@email.com");
        assertThat(user.getPassword()).isEqualTo("PASSWORD");
        assertThat(user.getNickname()).isEqualTo("nickname");
        assertThat(user.getStatus()).isEqualTo(UserStatus.PENDING);
        assertThat(user.getRole()).isEqualTo(Role.USER);
    }



}