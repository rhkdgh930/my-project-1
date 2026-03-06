package com.example.my_project_1.auth.service;

import com.example.my_project_1.auth.service.request.LoginRequest;
import com.example.my_project_1.auth.service.response.TokenResponse;

public interface AuthService {
    TokenResponse reissue(String refreshToken);
    TokenResponse restoreAccount(LoginRequest request);
    void logout(String accessToken);
}
