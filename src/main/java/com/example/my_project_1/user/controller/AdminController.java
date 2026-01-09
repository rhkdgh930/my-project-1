package com.example.my_project_1.user.controller;

import com.example.my_project_1.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminController {
    private final UserService userService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/test")
    public ResponseEntity<UserDetails> getMyInfo2(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(userDetails);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/suspend/{userId}")
    public ResponseEntity<Void> suspendUser(@AuthenticationPrincipal UserDetails userDetails, @PathVariable(name = "userId") Long userId) {
        userService.suspendUser(userId);
        return ResponseEntity.noContent().build();
    }
}
