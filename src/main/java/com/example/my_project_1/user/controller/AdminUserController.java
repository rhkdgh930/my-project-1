package com.example.my_project_1.user.controller;

import com.example.my_project_1.common.utils.PageResponse;
import com.example.my_project_1.common.exception.ExceptionResponse;
import com.example.my_project_1.common.exception.ValidExceptionResponse;
import com.example.my_project_1.user.service.AdminUserCommandService;
import com.example.my_project_1.user.service.AdminUserQueryService;
import com.example.my_project_1.user.service.request.UserSuspensionRequest;
import com.example.my_project_1.user.service.response.UserDetailResponse;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
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
            description = "관리자가 특정 유저를 일시 또는 영구 차단합니다. ADMIN 권한이 필요합니다. TEMPORARY 차단은 days가 필요하고 1 이상이어야 하며, PERMANENT 차단은 days를 사용하지 않습니다.",
            security = @SecurityRequirement(name = "jwtAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "유저 차단 성공"),
            @ApiResponse(responseCode = "400", description = "validation 실패 또는 유효하지 않은 차단 기간",
                    content = @Content(schema = @Schema(oneOf = {ValidExceptionResponse.class, ExceptionResponse.class}))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "403", description = "ADMIN 권한 필요",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "404", description = "사용자 없음",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @PostMapping("/{userId}/suspend")
    public ResponseEntity<Void> suspendUser(
            @Parameter(description = "차단할 유저 ID", example = "1", required = true)
            @PathVariable Long userId,
            @Valid @RequestBody UserSuspensionRequest request
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
            description = "관리자가 특정 유저의 차단을 즉시 해제합니다. ADMIN 권한이 필요합니다.",
            security = @SecurityRequirement(name = "jwtAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "유저 차단 해제 성공"),
            @ApiResponse(responseCode = "400", description = "차단 상태가 아닌 사용자 등 유효하지 않은 사용자 상태",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "403", description = "ADMIN 권한 필요",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "404", description = "사용자 없음",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @PostMapping("/{userId}/unsuspend")
    public ResponseEntity<Void> unSuspendUser(
            @Parameter(description = "차단을 복구할 유저 ID", example = "1", required = true)
            @PathVariable Long userId) {

        adminCommandService.unSuspendUser(userId);

        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "관리자용 유저 목록(Page)",
            description = "관리자용 유저 목록을 Spring Page 기반 PageResponse<UserDetailResponse> 형태로 조회합니다. ADMIN 권한이 필요합니다.",
            security = @SecurityRequirement(name = "jwtAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "유저 페이지 조회 성공",
                    content = @Content(schema = @Schema(implementation = PageResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "403", description = "ADMIN 권한 필요",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @GetMapping
    public ResponseEntity<PageResponse<UserDetailResponse>> readPage(
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(adminQueryService.findPage(pageable));
    }

    @Operation(
            summary = "관리자용 유저 목록(Cursor)",
            description = "lastId 이후의 유저 목록을 cursor 방식으로 조회합니다. 실제 응답은 List<UserDetailResponse>입니다. ADMIN 권한이 필요합니다.",
            security = @SecurityRequirement(name = "jwtAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "유저 cursor 목록 조회 성공",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = UserDetailResponse.class)))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "403", description = "ADMIN 권한 필요",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @GetMapping("/cursor")
    public ResponseEntity<List<UserDetailResponse>> readCursor(
            @Parameter(description = "마지막으로 조회한 유저 ID", example = "100", required = true)
            @RequestParam Long lastId,
            @Parameter(description = "조회 크기", example = "100")
            @RequestParam(defaultValue = "100") int size) {
        return ResponseEntity.ok(adminQueryService.findNext(lastId, size));
    }

    @Hidden
    @GetMapping("/test")
    public ResponseEntity<UserDetails> getMyInfo2(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(userDetails);
    }

}
