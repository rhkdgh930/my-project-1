package com.example.my_project_1.auth.exception;

import org.springframework.security.authentication.AuthenticationServiceException;

public class LoginFailException extends AuthenticationServiceException {
    public LoginFailException(String msg) {
        super(msg);
    }
}
