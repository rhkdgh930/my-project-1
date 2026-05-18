package com.example.my_project_1.report.controller;

import com.example.my_project_1.auth.userdetails.UserDetailsImpl;
import com.example.my_project_1.common.exception.ExceptionResponse;
import com.example.my_project_1.common.exception.ValidExceptionResponse;
import com.example.my_project_1.report.service.ReportService;
import com.example.my_project_1.report.service.request.ReportCreateRequest;
import com.example.my_project_1.report.service.response.ReportResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Report API", description = "신고 생성 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    @Operation(
            summary = "신고 생성",
            description = """
                    로그인 사용자가 게시글, 댓글, 유저를 신고합니다.
                    삭제된 게시글/댓글은 신고할 수 없고, 자기 자신 USER 신고는 허용하지 않습니다.
                    신고는 관리자 검토 대상으로 저장되며 자동 삭제나 자동 차단을 수행하지 않습니다.
                    """,
            security = @SecurityRequirement(name = "jwtAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "신고 생성 성공",
                    content = @Content(schema = @Schema(implementation = ReportResponse.class))),
            @ApiResponse(responseCode = "400", description = "검증 실패, 중복 신고, 자기 자신 신고",
                    content = @Content(schema = @Schema(oneOf = {ValidExceptionResponse.class, ExceptionResponse.class}))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "404", description = "신고 대상 없음",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @PostMapping
    public ResponseEntity<ReportResponse> create(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody @Valid ReportCreateRequest request
    ) {
        ReportResponse response = reportService.create(userDetails.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
