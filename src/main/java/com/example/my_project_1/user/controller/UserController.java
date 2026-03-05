package com.example.my_project_1.user.controller;

import com.example.my_project_1.auth.userdetails.UserDetailsImpl;
import com.example.my_project_1.user.service.UserCommandService;
import com.example.my_project_1.user.service.request.*;
import com.example.my_project_1.user.service.response.UserProfileResponse;
import com.example.my_project_1.user.service.response.UserSignUpResponse;
import com.example.my_project_1.user.service.response.UserWithdrawResponse;
import io.swagger.v3.oas.annotations.Operation;
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

@Tag(name = "USER API", description = "회원가입, 인증, 프로필 관리 등 일반 유저 기능 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserController {
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
            summary = "프로필 수정",
            description = "유저의 자기소개나 프로필 이미지 URL을 변경합니다."
    )
    @PreAuthorize("hasRole('USER')")
    @PatchMapping("/update-profile")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody UserProfileUpdateRequest request) {
        Long userId = userDetails.getUserId();
        UserProfileResponse response = userCommandService.updateProfile(userId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "회원 탈퇴 요청",
            description = "사용자가 탈퇴를 요청합니다. 즉시 삭제되지 않으며 7일간의 유예 기간 동안 '탈퇴 대기' 상태가 됩니다."
    )
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/withdraw")
    public ResponseEntity<UserWithdrawResponse> withdraw(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                                         @RequestBody UserWithdrawRequest request) {
        Long userId = userDetails.getUserId();
        UserWithdrawResponse response = userCommandService.withdraw(userId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "회원 탈퇴 취소",
            description = "유예 기간(7일) 내에 탈퇴 요청을 취소하고 계정을 정상 상태로 복구합니다."
    )
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/withdraw-cancel")
    public ResponseEntity<Void> cancelWithdraw(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        Long userId = userDetails.getUserId();
        userCommandService.cancelWithdraw(userId);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "비밀번호 변경 (로그인 상태)",
            description = "현재 비밀번호 확인 후 새로운 비밀번호로 업데이트합니다. 성공 시 모든 기기에서 로그아웃 처리됩니다."
    )
    @PreAuthorize("hasRole('USER')") // 로그인 필수
    @PatchMapping("/password")
    public ResponseEntity<Void> updatePassword(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody PasswordUpdateRequest request) {
        Long userId = userDetails.getUserId();
        userCommandService.updatePassword(userId, request);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "비밀번호 재설정 링크 발송",
            description = "비밀번호를 잊은 경우, 이메일로 비밀번호를 초기화할 수 있는 링크를 전송합니다."
    )
    @PostMapping("/password-reset/request")
    public ResponseEntity<Void> requestPasswordReset(@RequestParam @Email String email) {
        userCommandService.requestPasswordReset(email);
        return ResponseEntity.ok().build(); // 이메일이 없어도 항상 200 OK
    }

    @Operation(
            summary = "비밀번호 재설정 확인",
            description = "이메일로 받은 토큰을 사용하여 새로운 비밀번호를 설정합니다."
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
