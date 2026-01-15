package com.example.my_project_1.auth.service;

public interface EmailService {
    void sendVerificationCode(String toEmail, String code);
}
