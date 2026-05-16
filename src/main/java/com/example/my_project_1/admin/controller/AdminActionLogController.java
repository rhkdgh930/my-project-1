package com.example.my_project_1.admin.controller;

import com.example.my_project_1.admin.domain.AdminActionTargetType;
import com.example.my_project_1.admin.domain.AdminActionType;
import com.example.my_project_1.admin.service.AdminActionLogService;
import com.example.my_project_1.admin.service.response.AdminActionLogResponse;
import com.example.my_project_1.common.exception.ExceptionResponse;
import com.example.my_project_1.common.utils.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin Audit Log API", description = "관리자 조치 감사 로그 API")
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/admin/audit-logs")
public class AdminActionLogController {

    private final AdminActionLogService service;

    @Operation(
            summary = "관리자 감사 로그 목록 조회",
            description = "관리자 주요 조치 이력을 조회합니다. 감사 로그는 수정하거나 삭제하지 않습니다.",
            security = @SecurityRequirement(name = "jwtAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "감사 로그 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = PageResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "403", description = "ADMIN 권한 필요",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @GetMapping
    public ResponseEntity<PageResponse<AdminActionLogResponse>> readPage(
            @Parameter(description = "관리자 조치 유형")
            @RequestParam(required = false) AdminActionType actionType,
            @Parameter(description = "조치 대상 유형")
            @RequestParam(required = false) AdminActionTargetType targetType,
            @Parameter(description = "관리자 ID")
            @RequestParam(required = false) Long adminId,
            @ParameterObject
            @PageableDefault(size = 20)
            @SortDefault.SortDefaults({
                    @SortDefault(sort = "createdAt", direction = Sort.Direction.DESC),
                    @SortDefault(sort = "id", direction = Sort.Direction.DESC)
            })
            Pageable pageable
    ) {
        return ResponseEntity.ok(service.findLogs(actionType, targetType, adminId, pageable));
    }
}
