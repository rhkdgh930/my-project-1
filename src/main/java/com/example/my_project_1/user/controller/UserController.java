package com.example.my_project_1.user.controller;

import com.example.my_project_1.auth.userdetails.UserDetailsImpl;
import com.example.my_project_1.user.service.UserCommandService;
import com.example.my_project_1.user.service.request.*;
import com.example.my_project_1.user.service.response.UserProfileResponse;
import com.example.my_project_1.user.service.response.UserSignUpResponse;
import com.example.my_project_1.user.service.response.UserWithdrawResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User API", description = "회원가입, 이메일 인증, 프로필 관리 등 일반 사용자 기능")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserCommandService userCommandService;

    @Operation(
            summary = "이메일 인증 코드 발송",
            description = "회원가입 전 이메일 소유 확인을 위해 인증 코드를 발송합니다."
    )
    @PostMapping("/emails/verification")
    public ResponseEntity<Void> sendVerificationCode(
            @Parameter(description = "인증할 이메일")
            @RequestParam @Email String email) {

        userCommandService.sendVerificationCode(email);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "이메일 인증 코드 확인",
            description = "이메일과 인증 코드를 검증하여 인증을 완료합니다."
    )
    @PostMapping("/emails/verification/confirm")
    public ResponseEntity<String> verifyEmail(
            @Parameter(description = "이메일")
            @RequestParam String email,

            @Parameter(description = "이메일 인증 코드")
            @RequestParam String code) {

        userCommandService.verifyEmail(email, code);
        return ResponseEntity.ok("이메일 인증이 완료되었습니다.");
    }

    @Operation(
            summary = "회원가입",
            description = "인증된 이메일로 회원가입을 진행합니다."
    )
    @ApiResponse(responseCode = "201", description = "회원가입 성공")
    @PostMapping("/signup")
    public ResponseEntity<UserSignUpResponse> signUp(
            @Valid @RequestBody UserSignUpRequest request) {

        UserSignUpResponse response = userCommandService.signUp(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "프로필 수정",
            description = "자기소개 및 프로필 이미지를 수정합니다."
    )
    @PreAuthorize("hasRole('USER')")
    @PatchMapping("/me/profile")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody UserProfileUpdateRequest request) {

        Long userId = userDetails.getUserId();
        UserProfileResponse response = userCommandService.updateProfile(userId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "회원 탈퇴 요청",
            description = "회원 탈퇴를 요청합니다. 실제 삭제는 7일 후 배치 작업에서 처리됩니다."
    )
    @PreAuthorize("hasRole('USER')")
    @DeleteMapping("/me")
    public ResponseEntity<UserWithdrawResponse> withdraw(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody UserWithdrawRequest request) {

        Long userId = userDetails.getUserId();
        UserWithdrawResponse response = userCommandService.withdraw(userId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "비밀번호 변경",
            description = "현재 비밀번호 확인 후 새로운 비밀번호로 변경합니다."
    )
    @PreAuthorize("hasRole('USER')")
    @PatchMapping("/me/password")
    public ResponseEntity<Void> updatePassword(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody PasswordUpdateRequest request) {

        Long userId = userDetails.getUserId();
        userCommandService.updatePassword(userId, request);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "비밀번호 재설정 링크 발송",
            description = "비밀번호 재설정을 위한 이메일 링크를 발송합니다."
    )
    @PostMapping("/password-reset/request")
    public ResponseEntity<Void> requestPasswordReset(
            @Parameter(description = "비밀번호 재설정 요청 이메일")
            @RequestParam @Email String email) {

        userCommandService.requestPasswordReset(email);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "비밀번호 재설정",
            description = "이메일로 받은 토큰을 이용하여 비밀번호를 재설정합니다."
    )
    @PostMapping("/password-reset/confirm")
    public ResponseEntity<Void> confirmPasswordReset(
            @Valid @RequestBody PasswordResetRequest request) {

        userCommandService.resetPassword(request);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/test")
    public ResponseEntity<UserDetails> getMyInfo(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(userDetails);
    }
}