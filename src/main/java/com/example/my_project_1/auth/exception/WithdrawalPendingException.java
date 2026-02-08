package com.example.my_project_1.auth.exception;

import org.springframework.security.core.AuthenticationException;

public class WithdrawalPendingException extends AuthenticationException {
    public WithdrawalPendingException(String message) {
        super(message);
    }
}
