package com.example.my_project_1.user.controller;

import com.example.my_project_1.auth.userdetails.UserDetailsImpl;
import com.example.my_project_1.common.exception.ExceptionResponse;
import com.example.my_project_1.common.exception.ValidExceptionResponse;
import com.example.my_project_1.common.utils.PageResponse;
import com.example.my_project_1.post.service.PostQueryService;
import com.example.my_project_1.post.service.response.PostListResponse;
import com.example.my_project_1.user.service.UserCommandService;
import com.example.my_project_1.user.service.UserQueryService;
import com.example.my_project_1.user.service.request.*;
import com.example.my_project_1.user.service.response.UserMeResponse;
import com.example.my_project_1.user.service.response.UserProfileResponse;
import com.example.my_project_1.user.service.response.UserSignUpResponse;
import com.example.my_project_1.user.service.response.UserWithdrawResponse;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User API", description = "회원가입, 이메일 인증, 내 계정 조회/수정, 탈퇴 요청, 비밀번호 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserCommandService userCommandService;
    private final UserQueryService userQueryService;
    private final PostQueryService postQueryService;

    @Operation(
            summary = "이메일 인증 코드 발송",
            description = "회원가입 전에 이메일 소유 확인을 위한 인증 코드를 발송합니다. Public API입니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "인증 코드 발송 요청 성공"),
            @ApiResponse(responseCode = "400", description = "이메일 형식 오류",
                    content = @Content(schema = @Schema(implementation = ValidExceptionResponse.class)))
    })
    @PostMapping("/emails/verification")
    public ResponseEntity<Void> sendVerificationCode(
            @Parameter(description = "인증 코드를 받을 이메일", example = "user@example.com", required = true)
            @RequestParam @Email String email) {

        userCommandService.sendVerificationCode(email);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "이메일 인증 코드 확인",
            description = "이메일과 인증 코드를 검증해 회원가입 가능한 인증 상태로 표시합니다. Public API입니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "이메일 인증 성공",
                    content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "400", description = "인증 코드 만료, 불일치, 잘못된 입력",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @PostMapping("/emails/verification/confirm")
    public ResponseEntity<String> verifyEmail(
            @Parameter(description = "인증 대상 이메일", example = "user@example.com", required = true)
            @RequestParam String email,

            @Parameter(description = "이메일 인증 코드", example = "123456", required = true)
            @RequestParam String code) {

        userCommandService.verifyEmail(email, code);
        return ResponseEntity.ok("이메일 인증이 완료되었습니다.");
    }

    @Operation(
            summary = "회원가입",
            description = "이메일 인증이 완료된 계정으로 회원가입합니다. Public API입니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "회원가입 성공",
                    content = @Content(schema = @Schema(implementation = UserSignUpResponse.class))),
            @ApiResponse(responseCode = "400", description = "validation 실패, 이메일 미인증, 중복 이메일",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @PostMapping("/signup")
    public ResponseEntity<UserSignUpResponse> signUp(
            @Valid @RequestBody UserSignUpRequest request) {

        UserSignUpResponse response = userCommandService.signUp(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "내 정보 조회",
            description = "현재 로그인한 사용자의 기본 계정/프로필 정보를 조회합니다. password, refresh token, socialId, suspension 상세, withdrawal 상세는 포함하지 않습니다.",
            security = @SecurityRequirement(name = "jwtAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "내 정보 조회 성공",
                    content = @Content(schema = @Schema(implementation = UserMeResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "403", description = "차단 사용자",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "404", description = "사용자 없음",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @GetMapping("/me")
    public ResponseEntity<UserMeResponse> getMe(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        UserMeResponse response = userQueryService.getMe(userDetails.getUserId());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "내가 좋아요한 게시글 목록 조회",
            description = """
                    현재 로그인 사용자가 좋아요한 활성 게시글 목록을 조회합니다.
                    삭제된 게시글과 삭제된 게시판 아래 게시글은 제외하며,
                    정렬은 좋아요를 누른 최신순입니다.
                    viewCount는 Redis view delta를 더해 보정하고 likeCount는 DB denormalized count를 사용합니다.
                    """,
            security = @SecurityRequirement(name = "jwtAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "좋아요한 게시글 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = PageResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @GetMapping("/me/liked-posts")
    public ResponseEntity<PageResponse<PostListResponse>> getMyLikedPosts(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @ParameterObject Pageable pageable) {

        PageResponse<PostListResponse> response = postQueryService.getLikedPosts(
                userDetails.getUserId(),
                pageable
        );
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "내 프로필 수정",
            description = "자기소개와 프로필 이미지 URL을 수정합니다.",
            security = @SecurityRequirement(name = "jwtAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "프로필 수정 성공",
                    content = @Content(schema = @Schema(implementation = UserProfileResponse.class))),
            @ApiResponse(responseCode = "400", description = "validation 실패",
                    content = @Content(schema = @Schema(implementation = ValidExceptionResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "404", description = "사용자 없음",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PatchMapping("/me/profile")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody UserProfileUpdateRequest request) {

        Long userId = userDetails.getUserId();
        UserProfileResponse response = userCommandService.updateProfile(userId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "회원 탈퇴 요청",
            description = "즉시 물리 삭제가 아니라 탈퇴 요청 상태로 전환합니다. 탈퇴 완료는 배치에서 UserStatus.WITHDRAWN 및 개인정보 마스킹 정책으로 처리됩니다.",
            security = @SecurityRequirement(name = "jwtAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "탈퇴 요청 성공",
                    content = @Content(schema = @Schema(implementation = UserWithdrawResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패 또는 비밀번호 불일치",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "403", description = "USER 권한 필요",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "404", description = "사용자 없음",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @PreAuthorize("hasAnyRole('USER')")
    @DeleteMapping("/me")
    public ResponseEntity<UserWithdrawResponse> withdraw(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody UserWithdrawRequest request) {

        Long userId = userDetails.getUserId();
        UserWithdrawResponse response = userCommandService.withdraw(userId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "비밀번호 변경",
            description = "현재 비밀번호를 확인한 뒤 새 비밀번호로 변경합니다.",
            security = @SecurityRequirement(name = "jwtAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "비밀번호 변경 성공"),
            @ApiResponse(responseCode = "400", description = "validation 실패 또는 기존 비밀번호와 동일",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패 또는 현재 비밀번호 불일치",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "404", description = "사용자 없음",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @PreAuthorize("hasAnyRole('USER')")
    @PatchMapping("/me/password")
    public ResponseEntity<Void> updatePassword(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody PasswordUpdateRequest request) {

        Long userId = userDetails.getUserId();
        userCommandService.updatePassword(userId, request);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "비밀번호 재설정 링크 발송",
            description = "비밀번호 재설정을 위한 1회성 토큰 링크를 이메일로 발송합니다. Redis 기반 토큰을 사용하고 Outbox가 아닌 async mail event 흐름을 사용합니다. Public API입니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "비밀번호 재설정 요청 성공"),
            @ApiResponse(responseCode = "400", description = "이메일 형식 오류",
                    content = @Content(schema = @Schema(implementation = ValidExceptionResponse.class))),
            @ApiResponse(responseCode = "404", description = "사용자 없음",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @PostMapping("/password-reset/request")
    public ResponseEntity<Void> requestPasswordReset(
            @Parameter(description = "비밀번호 재설정 요청 이메일", example = "user@example.com", required = true)
            @RequestParam @Email String email) {

        userCommandService.requestPasswordReset(email);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "비밀번호 재설정",
            description = "이메일로 받은 1회성 토큰을 원자적으로 소비하고 새 비밀번호로 재설정합니다. Public API입니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "비밀번호 재설정 성공"),
            @ApiResponse(responseCode = "400", description = "validation 실패, 유효하지 않은 토큰, 기존 비밀번호와 동일",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "404", description = "사용자 없음",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @PostMapping("/password-reset/confirm")
    public ResponseEntity<Void> confirmPasswordReset(
            @Valid @RequestBody PasswordResetRequest request) {

        userCommandService.resetPassword(request);
        return ResponseEntity.ok().build();
    }

    @Hidden
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/test")
    public ResponseEntity<UserDetails> getMyInfo(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(userDetails);
    }
}
