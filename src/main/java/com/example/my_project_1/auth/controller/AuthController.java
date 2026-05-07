package com.example.my_project_1.auth.controller;

import com.example.my_project_1.auth.service.AuthService;
import com.example.my_project_1.auth.service.request.LoginRequest;
import com.example.my_project_1.auth.service.response.TokenResponse;
import com.example.my_project_1.auth.utils.CookieUtils;
import com.example.my_project_1.auth.utils.JwtProvider;
import com.example.my_project_1.common.exception.CustomException;
import com.example.my_project_1.common.exception.ErrorCode;
import com.example.my_project_1.common.exception.ExceptionResponse;
import com.example.my_project_1.common.exception.ValidExceptionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.example.my_project_1.auth.constant.SecurityConstants.AUTHORIZATION;
import static com.example.my_project_1.auth.constant.SecurityConstants.BEARER;
import static com.example.my_project_1.auth.constant.SecurityConstants.REFRESH_TOKEN;
import static com.example.my_project_1.auth.constant.SecurityConstants.REFRESH_TOKEN_COOKIE;

@Tag(name = "Auth API", description = "JWT login, token reissue, logout, and account restore APIs")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtProvider jwtProvider;

    // TODO 개발 단계에서는 Cookie/Header refresh token을 모두 허용한다. 운영 목표는 HttpOnly cookie 기반이다.

    @Operation(
            summary = "토큰 재발급",
            description = """
                    refreshToken cookie를 우선 사용하고, 없으면 Refresh-Token header를 fallback으로 사용합니다.
                    cookie와 header가 모두 있고 값이 다르면 INVALID_REFRESH_TOKEN으로 실패합니다.
                    성공 시 accessToken/refreshToken을 반환하고 새 refreshToken cookie를 내려줍니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "토큰 재발급 성공",
                    content = @Content(schema = @Schema(implementation = TokenResponse.class))),
            @ApiResponse(responseCode = "401", description = "유효하지 않거나 만료된 refresh token",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @PostMapping("/reissue")
    public ResponseEntity<TokenResponse> reissue(
            @Parameter(
                    name = REFRESH_TOKEN_COOKIE,
                    in = ParameterIn.COOKIE,
                    description = "Refresh token cookie. Header보다 우선합니다.",
                    required = false
            )
            @CookieValue(value = REFRESH_TOKEN_COOKIE, required = false) String refreshTokenCookie,
            @Parameter(
                    name = REFRESH_TOKEN,
                    in = ParameterIn.HEADER,
                    description = "Refresh token header fallback. Cookie가 없을 때 사용합니다.",
                    required = false
            )
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
            summary = "탈퇴 요청 계정 복구",
            description = "탈퇴 유예 기간 안의 계정을 이메일과 비밀번호로 복구하고 TokenResponse를 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "계정 복구 성공",
                    content = @Content(schema = @Schema(implementation = TokenResponse.class))),
            @ApiResponse(responseCode = "400", description = "요청 validation 실패",
                    content = @Content(schema = @Schema(implementation = ValidExceptionResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 정보 불일치",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "404", description = "사용자 없음",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @PostMapping("/restore")
    public ResponseEntity<TokenResponse> restore(
            @Valid @RequestBody LoginRequest request) {

        TokenResponse tokenResponse = authService.restoreAccount(request);
        return ResponseEntity.ok(tokenResponse);
    }

    @Operation(
            summary = "로그아웃",
            description = """
                    Authorization header는 optional입니다.
                    accessToken이 있으면 Authorization: Bearer accessToken header를 통해 blacklist 처리를 시도합니다.
                    accessToken이 없거나 만료되었으면 blacklist는 생략하고 logout을 계속 진행합니다.
                    refreshToken cookie 또는 Refresh-Token header가 있으면 refresh token hash 삭제를 시도합니다.
                    refreshToken이 없어도 성공하며, 성공 시 refreshToken cookie를 삭제하고 200 OK를 반환합니다.
                    잘못된 Bearer 형식 또는 유효하지 않은 refreshToken은 401로 응답합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그아웃 성공"),
            @ApiResponse(responseCode = "401", description = "잘못된 token 형식 또는 유효하지 않은 refreshToken",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @Parameter(
                    name = AUTHORIZATION,
                    in = ParameterIn.HEADER,
                    description = "Optional Bearer access token. 있으면 blacklist 처리를 시도합니다. 예: Bearer eyJhbGciOiJIUzI1NiJ9...",
                    required = false
            )
            @RequestHeader(value = AUTHORIZATION, required = false) String authorizationHeader,

            @Parameter(
                    name = REFRESH_TOKEN_COOKIE,
                    in = ParameterIn.COOKIE,
                    description = "Optional refresh token cookie.",
                    required = false
            )
            @CookieValue(value = REFRESH_TOKEN_COOKIE, required = false) String refreshTokenCookie,
            @Parameter(
                    name = REFRESH_TOKEN,
                    in = ParameterIn.HEADER,
                    description = "Optional refresh token header fallback.",
                    required = false
            )
            @RequestHeader(value = REFRESH_TOKEN, required = false) String refreshTokenHeader,

            HttpServletResponse response
    ) {
        String accessToken = resolveOptionalBearerToken(authorizationHeader);
        String refreshToken = resolveOptionalRefreshToken(refreshTokenCookie, refreshTokenHeader);

        authService.logout(accessToken, refreshToken);

        CookieUtils.deleteCookie(response, REFRESH_TOKEN_COOKIE);

        return ResponseEntity.ok().build();
    }

    private String resolveRefreshToken(String refreshTokenCookie, String refreshTokenHeader) {
        String refreshToken = resolveOptionalRefreshToken(refreshTokenCookie, refreshTokenHeader);

        if (!StringUtils.hasText(refreshToken)) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        return refreshToken;
    }

    private String resolveOptionalBearerToken(String authorizationHeader) {
        if (!StringUtils.hasText(authorizationHeader)) {
            return null;
        }

        if (!authorizationHeader.startsWith(BEARER)) {
            throw new CustomException(ErrorCode.INVALID_ACCESS_TOKEN);
        }

        return authorizationHeader.substring(BEARER.length());
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
