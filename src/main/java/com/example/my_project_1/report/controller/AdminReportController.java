package com.example.my_project_1.report.controller;

import com.example.my_project_1.auth.userdetails.UserDetailsImpl;
import com.example.my_project_1.common.exception.ExceptionResponse;
import com.example.my_project_1.common.exception.ValidExceptionResponse;
import com.example.my_project_1.common.utils.PageResponse;
import com.example.my_project_1.report.service.AdminModerationService;
import com.example.my_project_1.report.service.ReportService;
import com.example.my_project_1.report.service.request.ReportStatusUpdateRequest;
import com.example.my_project_1.report.service.response.ReportResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin Report API", description = "관리자 전용 신고 검토 API")
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/admin/reports")
public class AdminReportController {

    private final ReportService reportService;
    private final AdminModerationService adminModerationService;

    @Operation(
            summary = "신고 목록 조회",
            description = "관리자가 신고 목록을 조회합니다. 신고 내용은 관리자 전용 정보로 응답에 포함됩니다.",
            security = @SecurityRequirement(name = "jwtAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "신고 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = PageResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "403", description = "ADMIN 권한 필요",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @GetMapping
    public ResponseEntity<PageResponse<ReportResponse>> readPage(
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return ResponseEntity.ok(reportService.findReports(pageable));
    }

    @Operation(
            summary = "신고 상세 조회",
            description = "관리자가 단일 신고의 상세 내용을 조회합니다.",
            security = @SecurityRequirement(name = "jwtAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "신고 상세 조회 성공",
                    content = @Content(schema = @Schema(implementation = ReportResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "403", description = "ADMIN 권한 필요",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "404", description = "신고 없음",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @GetMapping("/{reportId}")
    public ResponseEntity<ReportResponse> read(
            @Parameter(description = "신고 ID", example = "1", required = true)
            @PathVariable Long reportId
    ) {
        return ResponseEntity.ok(reportService.findReport(reportId));
    }

    @Operation(
            summary = "신고 대상 삭제 조치",
            description = """
                    신고를 기준으로 대상 게시글 또는 댓글을 명시적으로 삭제하고 신고 상태를 ACTION_TAKEN으로 변경합니다.
                    Report status 변경 API 자체는 자동 삭제/정지를 수행하지 않습니다.
                    USER 신고 대상은 이 API에서 지원하지 않습니다.
                    """,
            security = @SecurityRequirement(name = "jwtAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "신고 대상 삭제 조치 성공",
                    content = @Content(schema = @Schema(implementation = ReportResponse.class))),
            @ApiResponse(responseCode = "400", description = "지원하지 않는 신고 대상",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "403", description = "ADMIN 권한 필요",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "404", description = "신고 또는 대상 없음",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @PostMapping("/{reportId}/actions/delete-target")
    public ResponseEntity<ReportResponse> deleteTarget(
            @Parameter(description = "신고 ID", example = "1", required = true)
            @PathVariable Long reportId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        return ResponseEntity.ok(adminModerationService.deleteTargetByReport(reportId, userDetails.getUserId()));
    }

    @Operation(
            summary = "신고 상태 변경",
            description = """
                    관리자가 신고 상태를 변경합니다.
                    ACTION_TAKEN 상태로 변경해도 게시글 삭제, 댓글 삭제, 사용자 차단은 자동 수행하지 않습니다.
                    """,
            security = @SecurityRequirement(name = "jwtAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "신고 상태 변경 성공",
                    content = @Content(schema = @Schema(implementation = ReportResponse.class))),
            @ApiResponse(responseCode = "400", description = "검증 실패",
                    content = @Content(schema = @Schema(oneOf = {ValidExceptionResponse.class, ExceptionResponse.class}))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "403", description = "ADMIN 권한 필요",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "404", description = "신고 없음",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @PatchMapping("/{reportId}/status")
    public ResponseEntity<ReportResponse> updateStatus(
            @Parameter(description = "신고 ID", example = "1", required = true)
            @PathVariable Long reportId,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody @Valid ReportStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(reportService.updateStatus(reportId, userDetails.getUserId(), request));
    }
}
