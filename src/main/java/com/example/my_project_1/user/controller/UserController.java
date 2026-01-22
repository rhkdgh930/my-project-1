package com.example.my_project_1.user.controller;

import com.example.my_project_1.user.service.UserCommandService;
import com.example.my_project_1.user.service.request.PasswordResetRequest;
import com.example.my_project_1.user.service.request.PasswordUpdateRequest;
import com.example.my_project_1.user.service.request.UserProfileUpdateRequest;
import com.example.my_project_1.user.service.request.UserSignUpRequest;
import com.example.my_project_1.user.service.response.UserProfileResponse;
import com.example.my_project_1.user.service.response.UserSignUpResponse;
import com.example.my_project_1.user.service.response.UserWithdrawResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserController {
    private final UserCommandService userCommandService;

    @PostMapping("/signup")
    public ResponseEntity<UserSignUpResponse> signUp(@Valid @RequestBody UserSignUpRequest request) {
        UserSignUpResponse response = userCommandService.signUp(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/verify")
    public ResponseEntity<String> verifyEmail(
            @RequestParam("email") String email,
            @RequestParam("code") String code
    ) {
        userCommandService.verifyEmail(email, code);
        return ResponseEntity.ok("이메일 인증이 완료되었습니다.");
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/test")
    public ResponseEntity<UserDetails> getMyInfo(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(userDetails);
    }

    @PreAuthorize("hasRole('USER')")
    @PatchMapping("/update-profile")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UserProfileUpdateRequest request) {
        Long userId = Long.valueOf(userDetails.getUsername());
        UserProfileResponse response = userCommandService.updateProfile(userId, request);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('USER')")
    @PostMapping("/withdraw")
    public ResponseEntity<UserWithdrawResponse> withdraw(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.valueOf(userDetails.getUsername());
        UserWithdrawResponse response = userCommandService.withdraw(userId);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('USER')") // 로그인 필수
    @PatchMapping("/password")
    public ResponseEntity<Void> updatePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody PasswordUpdateRequest request) {
        Long userId = Long.valueOf(userDetails.getUsername());
        userCommandService.updatePassword(userId, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/password-reset/request")
    public ResponseEntity<Void> requestPasswordReset(@RequestParam @Email String email) {
        userCommandService.requestPasswordReset(email);
        return ResponseEntity.ok().build(); // 이메일이 없어도 항상 200 OK
    }

    @PostMapping("/password-reset/confirm")
    public ResponseEntity<Void> confirmPasswordReset(
            @Valid @RequestBody PasswordResetRequest request) {

        userCommandService.resetPassword(request);
        return ResponseEntity.ok().build();
    }

}
