package com.example.my_project_1.auth.controller;

import com.example.my_project_1.auth.service.AuthService;
import com.example.my_project_1.auth.service.response.TokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.example.my_project_1.auth.constant.SecurityConstants.*;

@Tag(name = "Auth API", description = "토큰 재발급(Reissue) 및 로그아웃 등 인증 상태 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    @Operation(
            summary = "토큰 재발급 (Reissue)",
            description = "만료된 AccessToken을 대신하여 RefreshToken으로 새로운 토큰 세트를 발급받습니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "재발급 성공"),
            @ApiResponse(responseCode = "401", description = "유효하지 않거나 만료된 RefreshToken")
    })
    @PostMapping("/reissue")
    public ResponseEntity<TokenResponse> reissue(
            @RequestHeader(REFRESH_TOKEN) String refreshToken) {
        TokenResponse tokenResponse = authService.reissue(refreshToken);
        return ResponseEntity.ok(tokenResponse);
    }

    @Operation(
            summary = "로그아웃",
            description = "현재 사용 중인 AccessToken을 블랙리스트에 등록하고 서버 세션을 종료합니다."
    )
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader(AUTHORIZATION) String accessToken) {
        authService.logout(accessToken.replace(BEARER, ""));
        return ResponseEntity.ok().build();
    }
}
