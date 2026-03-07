package com.example.my_project_1.user.controller;

import com.example.my_project_1.user.service.AdminCommandService;
import com.example.my_project_1.user.service.AdminQueryService;
import com.example.my_project_1.user.service.request.UserSuspensionRequest;
import com.example.my_project_1.user.service.response.UserDetailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Admin API", description = "관리자 전용 API")
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminQueryService adminQueryService;
    private final AdminCommandService adminCommandService;

    @Operation(
            summary = "유저 차단",
            description = "관리자가 특정 유저를 일정 기간 동안 차단합니다."
    )
    @PostMapping("/users/{userId}/suspend")
    public ResponseEntity<Void> suspendUser(
            @Parameter(description = "차단할 유저 ID")
            @PathVariable Long userId,
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

    @Operation(
            summary = "전체 유저 조회",
            description = "관리자가 시스템에 등록된 모든 유저를 조회합니다."
    )
    @GetMapping("/users")
    public ResponseEntity<List<UserDetailResponse>> readAll() {

        List<UserDetailResponse> responses = adminQueryService.findAll();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/test")
    public ResponseEntity<UserDetails> getMyInfo2(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(userDetails);
    }

    //todo 차단 복구 로직 만들어야 함
}