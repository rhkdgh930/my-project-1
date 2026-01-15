package com.example.my_project_1.user.controller;

import com.example.my_project_1.user.service.AdminService;
import com.example.my_project_1.user.service.response.UserDetailResponse;
import com.example.my_project_1.user.service.response.UserProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService adminService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/test")
    public ResponseEntity<UserDetails> getMyInfo2(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(userDetails);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/suspend/{userId}")
    public ResponseEntity<Void> suspendUser(@AuthenticationPrincipal UserDetails userDetails, @PathVariable(name = "userId") Long userId) {
        adminService.suspendUser(userId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/readAllUsers")
    public ResponseEntity<List<UserDetailResponse>> readAll() {
        List<UserDetailResponse> responses = adminService.findAll();
        return ResponseEntity.ok(responses);
    }
}
