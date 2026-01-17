package com.example.my_project_1.auth.exception;

import org.springframework.security.authentication.AuthenticationServiceException;

public class LoginFailAuthenticationServiceException extends AuthenticationServiceException {
    public LoginFailAuthenticationServiceException(String msg) {
        super(msg);
    }
}
