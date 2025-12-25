package com.example.my_project_1.user.domain;

import com.example.my_project_1.user.service.request.UserSignUpRequest;

public class UserFixture {
    private final static String EMAIL = "email@email.com";
    private final static String PASSWORD = "password";
    private final static String NICKNAME = "nickname";

    public static UserSignUpRequest createUserSignUpRequest() {
        return UserSignUpRequest.create(EMAIL, PASSWORD, NICKNAME);
    }
}
