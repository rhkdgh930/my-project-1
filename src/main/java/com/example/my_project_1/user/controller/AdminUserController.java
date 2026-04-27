package com.example.my_project_1.user.controller;

import com.example.my_project_1.common.utils.PageResponse;
import com.example.my_project_1.user.service.AdminUserCommandService;
import com.example.my_project_1.user.service.AdminUserQueryService;
import com.example.my_project_1.user.service.request.UserSuspensionRequest;
import com.example.my_project_1.user.service.response.UserDetailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Admin User API", description = "관리자 전용 User API")
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserQueryService adminQueryService;
    private final AdminUserCommandService adminCommandService;

    @Operation(
            summary = "유저 차단",
            description = "관리자가 특정 유저를 일정 기간 동안 차단합니다."
    )
    @PostMapping("/{userId}/suspend")
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
            summary = "유저 차단 해제",
            description = "관리자가 특정 유저의 차단을 즉시 해제합니다."
    )
    @PostMapping("/{userId}/unsuspend")
    public ResponseEntity<Void> unSuspendUser(
            @Parameter(description = "차단을 복구할 유저 ID")
            @PathVariable Long userId) {

        adminCommandService.unSuspendUser(userId);

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "관리자용 유저 목록(Page)")
    @GetMapping
    public ResponseEntity<PageResponse<UserDetailResponse>> readPage(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(adminQueryService.findPage(pageable));
    }

    @Operation(summary = "관리자용 유저 목록(Cursor)")
    @GetMapping("/cursor")
    public ResponseEntity<List<UserDetailResponse>> readCursor(@RequestParam Long lastId,
                                                               @RequestParam(defaultValue = "100") int size) {
        return ResponseEntity.ok(adminQueryService.findNext(lastId, size));
    }

    @GetMapping("/test")
    public ResponseEntity<UserDetails> getMyInfo2(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(userDetails);
    }

}