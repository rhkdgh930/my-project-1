package com.example.my_project_1.auth.controller;

import com.example.my_project_1.auth.service.AuthService;
import com.example.my_project_1.auth.service.request.LoginRequest;
import com.example.my_project_1.auth.service.response.TokenResponse;
import com.example.my_project_1.user.service.UserCommandService;
import com.example.my_project_1.user.service.request.UserSignUpRequest;
import com.example.my_project_1.user.service.response.UserSignUpResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.example.my_project_1.auth.constant.SecurityConstants.*;

@Tag(name = "Auth API", description = "토큰 재발급(Reissue) 및 로그아웃 등 인증 상태 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final UserCommandService userCommandService;

    @Operation(
            summary = "이메일 인증 코드 발송",
            description = "회원가입 전 이메일 소유 확인을 위해 인증 코드를 발송합니다."
    )
    @PostMapping("/verification-code")
    public ResponseEntity<Void> sendVerificationCode(@RequestParam @Email String email) {
        userCommandService.sendVerificationCode(email);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "이메일 인증 코드 확인",
            description = "메일로 받은 코드와 이메일을 대조하여 인증을 완료합니다. 성공 시 Redis에 인증 완료 상태가 저장됩니다."
    )
    @PostMapping("/verify")
    public ResponseEntity<String> verifyEmail(
            @RequestParam("email") String email,
            @RequestParam("code") String code
    ) {
        userCommandService.verifyEmail(email, code);
        return ResponseEntity.ok("이메일 인증이 완료되었습니다. 회원가입을 진행해주세요.");
    }

    @Operation(
            summary = "회원가입",
            description = "인증이 완료된 이메일과 사용자 정보를 입력받아 회원가입을 완료합니다."
    )
    @PostMapping("/signup")
    public ResponseEntity<UserSignUpResponse> signUp(@Valid @RequestBody UserSignUpRequest request) {
        UserSignUpResponse response = userCommandService.signUp(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

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
            summary = "회원 탈퇴 취소",
            description = "유예 기간(7일) 내에 탈퇴 요청을 취소하고 계정을 정상 상태로 복구합니다."
    )
    @PostMapping("/restore")
    public ResponseEntity<TokenResponse> restore(@RequestBody LoginRequest request) {

        TokenResponse tokenResponse = authService.restoreAccount(request);

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
