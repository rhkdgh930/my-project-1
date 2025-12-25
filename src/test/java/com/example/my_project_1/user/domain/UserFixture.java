package com.example.my_project_1.user.domain;

import com.example.my_project_1.user.service.request.UserSignUpRequest;

public class UserFixture {
    private final static String EMAIL = "email@email.com";
    private final static String PASSWORD = "password";
    private final static String NICKNAME = "nickname";

    public static UserSignUpRequest createUserSignUpRequest() {
        return UserSignUpRequest.create(EMAIL, PASSWORD, NICKNAME);
    }

    public static PasswordEncoder createPasswordEncoder() {
        return new PasswordEncoder() {
            @Override
            public String encode(String password) {
                return password.toUpperCase();
            }

            @Override
            public boolean matches(String password, String passwordHash) {
                return encode(password).equals(passwordHash);
            }
        };
    }
}
