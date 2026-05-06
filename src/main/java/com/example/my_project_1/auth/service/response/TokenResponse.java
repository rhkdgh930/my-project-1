package com.example.my_project_1.auth.service.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "JWT 토큰 발급 응답")
public class TokenResponse {
    @Schema(description = "API 인증에 사용하는 access token", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String accessToken;

    @Schema(description = "토큰 재발급에 사용하는 refresh token. 운영 목표는 HttpOnly cookie 기반입니다.", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String refreshToken;
}
