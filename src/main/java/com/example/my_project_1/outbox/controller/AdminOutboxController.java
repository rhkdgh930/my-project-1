package com.example.my_project_1.outbox.controller;

import com.example.my_project_1.common.exception.ExceptionResponse;
import com.example.my_project_1.common.utils.PageResponse;
import com.example.my_project_1.outbox.domain.OutboxStatus;
import com.example.my_project_1.outbox.service.AdminOutboxService;
import com.example.my_project_1.outbox.service.response.AdminOutboxResponse;
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
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin Outbox API", description = "관리자 전용 Outbox 운영 API")
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/admin/outbox")
public class AdminOutboxController {

    private final AdminOutboxService adminOutboxService;

    @Operation(
            summary = "Outbox 이벤트 목록 조회",
            description = "관리자가 Outbox 이벤트 목록을 조회합니다. payload는 응답에 포함하지 않습니다.",
            security = @SecurityRequirement(name = "jwtAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Outbox 이벤트 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = PageResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "403", description = "ADMIN 권한 필요",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @GetMapping
    public ResponseEntity<PageResponse<AdminOutboxResponse>> readPage(
            @Parameter(description = "Outbox 상태 필터", example = "FAILED")
            @RequestParam(required = false) OutboxStatus status,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return ResponseEntity.ok(adminOutboxService.findPage(status, pageable));
    }

    @Operation(
            summary = "Outbox 이벤트 재시도 예약",
            description = """
                    FAILED 또는 DEAD 상태의 Outbox event를 재처리 가능한 PENDING 상태로 되돌립니다.
                    SUCCESS 상태는 이미 성공 처리된 이벤트이므로 재시도할 수 없습니다.
                    PENDING 상태는 이미 재처리 대기 중이므로 다시 예약할 수 없습니다.
                    PROCESSING 상태는 현재 처리 중이므로 재시도할 수 없습니다.
                    내부 운영용 API이며 payload 구조는 노출하지 않습니다.
                    ADMIN 권한이 필요합니다.
                    """,
            security = @SecurityRequirement(name = "jwtAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "재시도 예약 성공"),
            @ApiResponse(responseCode = "400", description = """
                    재시도 불가능 상태.
                    OUTBOX_ALREADY_SUCCEEDED: 이미 성공 처리된 이벤트
                    OUTBOX_ALREADY_PENDING: 이미 재처리 대기 중인 이벤트
                    OUTBOX_RETRY_NOT_ALLOWED: 기타 재시도 불가능 상태
                    """,
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "403", description = "ADMIN 권한 필요",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "404", description = "Outbox event 없음. OUTBOX_EVENT_NOT_FOUND",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "409", description = "현재 처리 중인 이벤트. OUTBOX_ALREADY_PROCESSING",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @PostMapping("/{id}/retry")
    public ResponseEntity<Void> retry(
            @Parameter(description = "Outbox event ID", example = "1", required = true)
            @PathVariable Long id
    ) {
        adminOutboxService.retry(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Outbox 이벤트 즉시 재시도",
            description = """
                    FAILED 또는 DEAD 상태의 Outbox event를 PENDING으로 되돌린 뒤 가능한 경우 즉시 processor 실행을 시도합니다.
                    요청 수락 성공 시 202 Accepted를 반환하며, 실제 side effect 성공 여부는 Outbox 처리 결과에 따릅니다.
                    SUCCESS 상태는 이미 성공 처리된 이벤트이므로 재시도할 수 없습니다.
                    PENDING 상태는 이미 재처리 대기 중이므로 다시 예약할 수 없습니다.
                    PROCESSING 상태는 현재 처리 중이므로 재시도할 수 없습니다.
                    내부 운영용 API이며 payload 구조는 노출하지 않습니다.
                    ADMIN 권한이 필요합니다.
                    """,
            security = @SecurityRequirement(name = "jwtAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "즉시 재시도 요청 수락"),
            @ApiResponse(responseCode = "400", description = """
                    재시도 불가능 상태.
                    OUTBOX_ALREADY_SUCCEEDED: 이미 성공 처리된 이벤트
                    OUTBOX_ALREADY_PENDING: 이미 재처리 대기 중인 이벤트
                    OUTBOX_RETRY_NOT_ALLOWED: 기타 재시도 불가능 상태
                    """,
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "403", description = "ADMIN 권한 필요",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "404", description = "Outbox event 없음. OUTBOX_EVENT_NOT_FOUND",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "409", description = "현재 처리 중인 이벤트. OUTBOX_ALREADY_PROCESSING",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @PostMapping("/{id}/retry-now")
    public ResponseEntity<Void> retryNow(
            @Parameter(description = "Outbox event ID", example = "1", required = true)
            @PathVariable Long id
    ) {
        adminOutboxService.retryNow(id);
        return ResponseEntity.accepted().build();
    }
}
