package com.example.my_project_1.auth.controller;

import com.example.my_project_1.auth.service.AuthService;
import com.example.my_project_1.auth.service.request.LoginRequest;
import com.example.my_project_1.auth.service.response.TokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.example.my_project_1.auth.constant.SecurityConstants.*;

@Tag(name = "Auth API", description = "토큰 재발급(Reissue) 및 로그아웃 등 인증 상태 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    @Operation(
            summary = "토큰 재발급",
            description = "RefreshToken을 사용하여 새로운 AccessToken과 RefreshToken을 발급합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "토큰 재발급 성공"),
            @ApiResponse(responseCode = "401", description = "유효하지 않은 RefreshToken")
    })
    @PostMapping("/reissue")
    public ResponseEntity<TokenResponse> reissue(
            @Parameter(description = "Refresh Token", required = true)
            @RequestHeader(REFRESH_TOKEN) String refreshToken) {

        TokenResponse tokenResponse = authService.reissue(refreshToken);
        return ResponseEntity.ok(tokenResponse);
    }

    @Operation(
            summary = "회원 탈퇴 취소",
            description = "탈퇴 요청 후 7일 유예 기간 내에 계정을 복구합니다."
    )
    @PostMapping("/restore")
    public ResponseEntity<TokenResponse> restore(
            @Valid @RequestBody LoginRequest request) {

        TokenResponse tokenResponse = authService.restoreAccount(request);
        return ResponseEntity.ok(tokenResponse);
    }

    @Operation(
            summary = "로그아웃",
            description = "AccessToken을 블랙리스트에 등록하고 RefreshToken을 제거합니다."
    )
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @Parameter(description = "Bearer AccessToken", required = true)
            @RequestHeader(AUTHORIZATION) String accessToken) {

        authService.logout(accessToken.replace(BEARER, ""));
        return ResponseEntity.ok().build();
    }
}