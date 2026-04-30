package com.example.my_project_1.auth.controller;

import com.example.my_project_1.auth.service.AuthService;
import com.example.my_project_1.auth.service.request.LoginRequest;
import com.example.my_project_1.auth.service.response.TokenResponse;
import com.example.my_project_1.auth.utils.CookieUtils;
import com.example.my_project_1.auth.utils.JwtProvider;
import com.example.my_project_1.common.exception.CustomException;
import com.example.my_project_1.common.exception.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import static com.example.my_project_1.auth.constant.SecurityConstants.*;

@Tag(name = "Auth API", description = "토큰 재발급(Reissue) 및 로그아웃 등 인증 상태 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtProvider jwtProvider;

    //TODO 개발단계이기 때문에 Cookie, Header방식을 모두 사용 중, 운영단계에서는 수정 해야 함!!!

    @Operation(
            summary = "토큰 재발급",
            description = "RefreshToken 쿠키 또는 Refresh-Token 헤더를 사용하여 새로운 AccessToken과 RefreshToken을 발급합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "토큰 재발급 성공"),
            @ApiResponse(responseCode = "401", description = "유효하지 않은 RefreshToken")
    })
    @PostMapping("/reissue")
    public ResponseEntity<TokenResponse> reissue(
            @CookieValue(value = REFRESH_TOKEN_COOKIE, required = false) String refreshTokenCookie,
            @RequestHeader(value = REFRESH_TOKEN, required = false) String refreshTokenHeader,
            HttpServletResponse response
    ) {
        String refreshToken = resolveRefreshToken(refreshTokenCookie, refreshTokenHeader);

        TokenResponse tokenResponse = authService.reissue(refreshToken);

        int refreshMaxAge =
                (int) (jwtProvider.getRemainingValidityMillis(tokenResponse.getRefreshToken()) / 1000);

        CookieUtils.addCookie(
                response,
                REFRESH_TOKEN_COOKIE,
                tokenResponse.getRefreshToken(),
                refreshMaxAge
        );

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
            @RequestHeader(AUTHORIZATION) String authorizationHeader,

            @CookieValue(value = REFRESH_TOKEN_COOKIE, required = false) String refreshTokenCookie,
            @RequestHeader(value = REFRESH_TOKEN, required = false) String refreshTokenHeader,

            HttpServletResponse response
    ) {
        String accessToken = resolveBearerToken(authorizationHeader);
        String refreshToken = resolveOptionalRefreshToken(refreshTokenCookie, refreshTokenHeader);

        authService.logout(accessToken, refreshToken);

        CookieUtils.deleteCookie(response, REFRESH_TOKEN_COOKIE);

        return ResponseEntity.ok().build();
    }

    private String resolveBearerToken(String authorizationHeader) {
        if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith(BEARER)) {
            throw new CustomException(ErrorCode.INVALID_ACCESS_TOKEN);
        }

        return authorizationHeader.substring(BEARER.length());
    }

    private String resolveRefreshToken(String refreshTokenCookie, String refreshTokenHeader) {
        String refreshToken = resolveOptionalRefreshToken(refreshTokenCookie, refreshTokenHeader);

        if (!StringUtils.hasText(refreshToken)) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        return refreshToken;
    }

    private String resolveOptionalRefreshToken(String refreshTokenCookie, String refreshTokenHeader) {
        boolean hasCookie = StringUtils.hasText(refreshTokenCookie);
        boolean hasHeader = StringUtils.hasText(refreshTokenHeader);

        if (hasCookie && hasHeader) {
            if (!refreshTokenCookie.equals(refreshTokenHeader)) {
                throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
            }

            return refreshTokenCookie;
        }

        if (hasCookie) {
            return refreshTokenCookie;
        }

        if (hasHeader) {
            return refreshTokenHeader;
        }

        return null;
    }
}
