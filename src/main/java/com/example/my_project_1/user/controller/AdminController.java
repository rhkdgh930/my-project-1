package com.example.my_project_1.user.controller;

import com.example.my_project_1.user.service.AdminCommandService;
import com.example.my_project_1.user.service.AdminQueryService;
import com.example.my_project_1.user.service.request.UserSuspensionRequest;
import com.example.my_project_1.user.service.response.UserDetailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminQueryService adminQueryService;
    private final AdminCommandService adminCommandService;

    @GetMapping("/test")
    public ResponseEntity<UserDetails> getMyInfo2(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(userDetails);
    }

    @PostMapping("/suspend/{userId}")
    public ResponseEntity<Void> suspendUser(
            @PathVariable(name = "userId") Long userId,
            @RequestBody UserSuspensionRequest request
    ) {
        adminCommandService.suspendUser(
                userId,
                request.getType(),
                request.getReason(),
                request.getDuration()
        );
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/readAllUsers")
    public ResponseEntity<List<UserDetailResponse>> readAll() {
        List<UserDetailResponse> responses = adminQueryService.findAll();
        return ResponseEntity.ok(responses);
    }
}
