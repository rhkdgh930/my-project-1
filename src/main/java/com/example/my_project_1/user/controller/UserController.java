package com.example.my_project_1.user.controller;

import com.example.my_project_1.user.domain.User;
import com.example.my_project_1.user.service.UserService;
import com.example.my_project_1.user.service.request.UserProfileUpdateRequest;
import com.example.my_project_1.user.service.request.UserSignUpRequest;
import com.example.my_project_1.user.service.response.UserDetailResponse;
import com.example.my_project_1.user.service.response.UserSignUpResponse;
import jakarta.validation.Valid;
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
    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<UserSignUpResponse> signUp(@Valid @RequestBody UserSignUpRequest request) {
        UserSignUpResponse response = userService.signUp(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/test")
    public ResponseEntity<UserDetails> getMyInfo(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(userDetails);
    }

    @PatchMapping("/update-profile")
    public ResponseEntity<UserDetailResponse> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UserProfileUpdateRequest request) {
        Long userId = Long.valueOf(userDetails.getUsername());
        UserDetailResponse response = userService.updateProfile(userId, request);
        return ResponseEntity.ok(response);
    }

}
