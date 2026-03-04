package com.example.my_project_1.auth.controller;

import com.example.my_project_1.auth.service.AuthService;
import com.example.my_project_1.auth.service.response.TokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.example.my_project_1.auth.constant.SecurityConstants.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/reissue")
    public ResponseEntity<TokenResponse> reissue(
            @RequestHeader(REFRESH_TOKEN) String refreshToken) {
        TokenResponse tokenResponse = authService.reissue(refreshToken);
        return ResponseEntity.ok(tokenResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader(AUTHORIZATION) String accessToken) {
        authService.logout(accessToken.replace(BEARER, ""));
        return ResponseEntity.ok().build();
    }
}
