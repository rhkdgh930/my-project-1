package com.example.my_project_1.user.domain;

public interface PasswordEncoder {
    String encode(String password);

    boolean matches(String password, String passwordHash);
}
