package com.example.my_project_1.auth.exception;

import org.springframework.security.core.AuthenticationException;

public abstract class WithdrawalException extends AuthenticationException {
    public WithdrawalException(String message) {
        super(message);
    }
}
