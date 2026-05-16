package com.example.my_project_1.auth.config;

import com.example.my_project_1.auth.filter.JwtAuthenticationFilter;
import com.example.my_project_1.auth.handler.*;
import com.example.my_project_1.auth.controller.AuthController;
import com.example.my_project_1.auth.oauth.CustomOAuth2UserService;
import com.example.my_project_1.auth.service.*;
import com.example.my_project_1.auth.service.response.TokenResponse;
import com.example.my_project_1.auth.userdetails.UserDetailsImpl;
import com.example.my_project_1.auth.utils.AuthTokenResolver;
import com.example.my_project_1.auth.utils.CookieManager;
import com.example.my_project_1.auth.utils.CookieProperties;
import com.example.my_project_1.auth.utils.JwtProvider;
import com.example.my_project_1.board.controller.AdminBoardController;
import com.example.my_project_1.board.controller.BoardController;
import com.example.my_project_1.board.service.BoardCommandService;
import com.example.my_project_1.board.service.BoardQueryService;
import com.example.my_project_1.comment.controller.CommentController;
import com.example.my_project_1.comment.service.CommentCommandService;
import com.example.my_project_1.comment.service.CommentQueryService;
import com.example.my_project_1.common.exception.CustomException;
import com.example.my_project_1.common.exception.ErrorCode;
import com.example.my_project_1.common.exception.ErrorResponseWriter;
import com.example.my_project_1.common.logging.HttpLoggingFilter;
import com.example.my_project_1.common.logging.SecurityUserMdcFilter;
import com.example.my_project_1.common.logging.TraceIdFilter;
import com.example.my_project_1.common.utils.PageResponse;
import com.example.my_project_1.image.controller.ImageController;
import com.example.my_project_1.image.service.ImageStorage;
import com.example.my_project_1.image.service.ImageUploadService;
import com.example.my_project_1.outbox.controller.AdminOutboxController;
import com.example.my_project_1.outbox.domain.OutboxEvent;
import com.example.my_project_1.outbox.domain.OutboxEventType;
import com.example.my_project_1.outbox.service.AdminOutboxService;
import com.example.my_project_1.outbox.service.response.AdminOutboxDetailResponse;
import com.example.my_project_1.post.controller.PostController;
import com.example.my_project_1.post.controller.TagPostController;
import com.example.my_project_1.post.service.PostCommandService;
import com.example.my_project_1.post.service.PostQueryService;
import com.example.my_project_1.post.service.request.PostSearchCondition;
import com.example.my_project_1.report.controller.AdminReportController;
import com.example.my_project_1.report.controller.AdminModerationController;
import com.example.my_project_1.report.controller.ReportController;
import com.example.my_project_1.report.domain.ReportStatus;
import com.example.my_project_1.report.domain.ReportTargetType;
import com.example.my_project_1.report.service.ReportService;
import com.example.my_project_1.report.service.AdminModerationService;
import com.example.my_project_1.report.service.request.ReportStatusUpdateRequest;
import com.example.my_project_1.report.service.response.ReportResponse;
import com.example.my_project_1.user.controller.UserController;
import com.example.my_project_1.user.domain.AccountStatus;
import com.example.my_project_1.user.domain.SuspensionReason;
import com.example.my_project_1.user.domain.SuspensionType;
import com.example.my_project_1.user.domain.UserStatus;
import com.example.my_project_1.user.service.UserCommandService;
import com.example.my_project_1.user.service.UserQueryService;
import com.example.my_project_1.user.service.response.UserMeResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        BoardController.class,
        AdminBoardController.class,
        PostController.class,
        TagPostController.class,
        CommentController.class,
        ImageController.class,
        AuthController.class,
        UserController.class,
        AdminOutboxController.class,
        ReportController.class,
        AdminModerationController.class,
        AdminReportController.class
})
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtAuthenticationEntryPoint.class,
        JwtAccessDeniedHandler.class,
        TraceIdFilter.class,
        SecurityUserMdcFilter.class,
        HttpLoggingFilter.class,
        PasswordConfig.class,
        UserAccountPolicy.class,
        CookieProperties.class,
        CookieManager.class,
        AuthTokenResolver.class,
        ErrorResponseWriter.class
})
class BoardPostCommentImageSecurityConfigTest {

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @MockitoBean
    private BoardCommandService boardCommandService;

    @MockitoBean
    private BoardQueryService boardQueryService;

    @MockitoBean
    private PostCommandService postCommandService;

    @MockitoBean
    private PostQueryService postQueryService;

    @MockitoBean
    private CommentCommandService commentCommandService;

    @MockitoBean
    private CommentQueryService commentQueryService;

    @MockitoBean
    private ImageUploadService imageUploadService;

    @MockitoBean
    private ImageStorage imageStorage;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UserCommandService userCommandService;

    @MockitoBean
    private UserQueryService userQueryService;

    @MockitoBean
    private AdminOutboxService adminOutboxService;

    @MockitoBean
    private ReportService reportService;

    @MockitoBean
    private AdminModerationService adminModerationService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private RedisTokenService redisTokenService;

    @MockitoBean
    private RedisUserContextService redisUserContextService;

    @MockitoBean
    private RedisLoginAttemptService redisLoginAttemptService;

    @MockitoBean
    private JwtLoginSuccessHandler jwtLoginSuccessHandler;

    @MockitoBean
    private JwtLoginFailureHandler jwtLoginFailureHandler;

    @MockitoBean
    private OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    @MockitoBean
    private OAuth2LoginFailureHandler oAuth2LoginFailureHandler;

    @MockitoBean
    private CustomOAuth2UserService customOAuth2UserService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Board, Post, Comment 조회 API는 인증 없이 접근할 수 있다.")
    void readApis_arePermitAll() throws Exception {
        when(boardQueryService.findAllBoards()).thenReturn(List.of());
        when(postQueryService.getPosts(eq(1L), any(PostSearchCondition.class), any(Pageable.class)))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0, true));
        when(postQueryService.getPostsByTagName(eq("Spring"), any(Pageable.class)))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0, true));
        when(commentQueryService.getComments(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/boards"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/boards/1"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/boards/1/posts"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/boards/1/posts/popular"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/boards/1/posts/1"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/tags/Spring/posts"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/posts/1/comments"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Post, Comment, Image write API는 인증 없이 접근할 수 없다.")
    void writeApis_requireAuthentication() throws Exception {
        mockMvc.perform(post("/api/boards/1/posts")
                        .contentType("application/json")
                        .content("{\"title\":\"title\",\"content\":\"content\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/boards/1/posts/1")
                        .contentType("application/json")
                        .content("{\"title\":\"title\",\"content\":\"content\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/boards/1/posts/1"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/boards/1/posts/1/like"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/boards/1/posts/1/like"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/posts/1/comments")
                        .contentType("application/json")
                        .content("{\"content\":\"comment\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/posts/1/comments/1/replies")
                        .contentType("application/json")
                        .content("{\"content\":\"reply\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/posts/1/comments/10")
                        .contentType("application/json")
                        .content("{\"content\":\"updated\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/posts/1/comments/10"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(multipart("/api/images")
                        .file("file", "image".getBytes()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Admin Board API는 일반 사용자 권한으로 접근할 수 없다.")
    @WithMockUser(roles = "USER")
    void adminBoardApis_requireAdminRole() throws Exception {
        mockMvc.perform(get("/api/admin/boards"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/admin/boards")
                        .contentType("application/json")
                        .content("{\"name\":\"board\",\"description\":\"description\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin Outbox 상세 조회 API는 ADMIN 권한이 필요하고 payload를 포함한다.")
    void adminOutboxDetailApi_requiresAdminRoleAndReturnsPayload() throws Exception {
        when(adminOutboxService.findById(1L))
                .thenReturn(AdminOutboxDetailResponse.from(outboxEvent(1L)));

        mockMvc.perform(get("/api/admin/outbox/1"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/outbox/1")
                        .with(authentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                                userDetails(),
                                null,
                                userDetails().getAuthorities()
                        ))))
                .andExpect(status().isForbidden());

        UserDetailsImpl adminDetails = userDetails("ADMIN");
        mockMvc.perform(get("/api/admin/outbox/1")
                        .with(authentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                                adminDetails,
                                null,
                                adminDetails.getAuthorities()
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.eventType").value(OutboxEventType.USER_ACCOUNT_CHANGED.name()))
                .andExpect(jsonPath("$.eventKey").value("event-key"))
                .andExpect(jsonPath("$.payload").value("{\"userId\":1}"));
    }

    @Test
    @DisplayName("Auth, User public API는 인증 없이 SecurityConfig를 통과한다.")
    void authAndUserPublicApis_areNotBlockedBySecurityConfig() throws Exception {
        TokenResponse tokenResponse = new TokenResponse("access-token", "refresh-token");
        when(authService.reissue("refresh-token")).thenReturn(tokenResponse);
        when(jwtProvider.getRemainingValidityMillis("refresh-token")).thenReturn(60_000L);

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(result -> assertThat(result.getResponse().getStatus())
                        .isNotEqualTo(HttpStatus.UNAUTHORIZED.value()));
        mockMvc.perform(post("/api/auth/reissue")
                        .header("Refresh-Token", "refresh-token"))
                .andExpect(result -> assertThat(result.getResponse().getStatus())
                        .isNotEqualTo(HttpStatus.UNAUTHORIZED.value()));
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/restore")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(result -> assertThat(result.getResponse().getStatus())
                        .isNotEqualTo(HttpStatus.UNAUTHORIZED.value()));
        mockMvc.perform(post("/api/users/signup")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(result -> assertThat(result.getResponse().getStatus())
                        .isNotEqualTo(HttpStatus.UNAUTHORIZED.value()));
        mockMvc.perform(post("/api/users/emails/verification")
                        .param("email", "test@example.com"))
                .andExpect(result -> assertThat(result.getResponse().getStatus())
                        .isNotEqualTo(HttpStatus.UNAUTHORIZED.value()));
        mockMvc.perform(post("/api/users/emails/verification/confirm")
                        .param("email", "test@example.com")
                        .param("code", "123456"))
                .andExpect(result -> assertThat(result.getResponse().getStatus())
                        .isNotEqualTo(HttpStatus.UNAUTHORIZED.value()));
        mockMvc.perform(post("/api/users/password-reset/request")
                        .param("email", "test@example.com"))
                .andExpect(result -> assertThat(result.getResponse().getStatus())
                        .isNotEqualTo(HttpStatus.UNAUTHORIZED.value()));
        mockMvc.perform(post("/api/users/password-reset/confirm")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(result -> assertThat(result.getResponse().getStatus())
                        .isNotEqualTo(HttpStatus.UNAUTHORIZED.value()));
    }

    @Test
    @DisplayName("Post delete API는 인증 사용자가 호출하면 204를 반환한다.")
    void postDeleteApi_returnsNoContentForAuthenticatedUser() throws Exception {
        UserDetailsImpl userDetails = userDetails();

        mockMvc.perform(delete("/api/boards/1/posts/10")
                        .with(authentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        ))))
                .andExpect(status().isNoContent());

        verify(postCommandService).delete(1L, 10L, 1L);
    }

    @Test
    @DisplayName("Comment update API는 인증 사용자가 호출하면 command service를 호출한다.")
    void commentUpdateApi_callsServiceForAuthenticatedUser() throws Exception {
        UserDetailsImpl userDetails = userDetails();

        mockMvc.perform(patch("/api/posts/1/comments/10")
                        .contentType("application/json")
                        .content("{\"content\":\"updated\"}")
                        .with(authentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        ))))
                .andExpect(status().isOk());

        verify(commentCommandService).update(1L, 10L, 1L, "updated");
    }

    @Test
    @DisplayName("Comment delete API는 인증 사용자가 호출하면 204를 반환한다.")
    void commentDeleteApi_returnsNoContentForAuthenticatedUser() throws Exception {
        UserDetailsImpl userDetails = userDetails();

        mockMvc.perform(delete("/api/posts/1/comments/10")
                        .with(authentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        ))))
                .andExpect(status().isNoContent());

        verify(commentCommandService).delete(1L, 10L, 1L);
    }

    @Test
    @DisplayName("Image upload에 실패하면 400에러를 던진다.")
    void imageUploadValidationFailure_returnsBadRequest() throws Exception {
        UserDetailsImpl userDetails = userDetails();
        when(imageUploadService.upload(any(), eq(1L)))
                .thenThrow(new CustomException(ErrorCode.INVALID_IMAGE_FILE));

        mockMvc.perform(multipart("/api/images")
                        .file("file", "not-image".getBytes())
                        .with(authentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_IMAGE_FILE.name()));
    }

    @Test
    @DisplayName("사용자 정보 조회 API는 인증이 필요하고 인증 사용자는 접근할 수 있다.")
    void userMeApi_requiresAuthenticationAndAllowsAuthenticatedUser() throws Exception {
        UserMeResponse response = new UserMeResponse();
        when(userQueryService.getMe(anyLong())).thenReturn(response);

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/users/me")
                        .with(authentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                                userDetails(),
                        null,
                        userDetails().getAuthorities()
                ))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("내가 좋아요한 게시글 목록 API는 인증이 필요하고 인증 사용자는 접근할 수 있다.")
    void likedPostsApi_requiresAuthenticationAndAllowsAuthenticatedUser() throws Exception {
        when(postQueryService.getLikedPosts(anyLong(), any(Pageable.class)))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0, true));

        mockMvc.perform(get("/api/users/me/liked-posts"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/users/me/liked-posts")
                        .with(authentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                                userDetails(),
                                null,
                                userDetails().getAuthorities()
                        ))))
                .andExpect(status().isOk());

        verify(postQueryService).getLikedPosts(eq(1L), any(Pageable.class));
    }

    @Test
    @DisplayName("내가 작성한 게시글 목록 API는 인증이 필요하고 인증 사용자는 접근할 수 있다.")
    void myPostsApi_requiresAuthenticationAndAllowsAuthenticatedUser() throws Exception {
        when(postQueryService.getMyPosts(anyLong(), any(Pageable.class)))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0, true));

        mockMvc.perform(get("/api/users/me/posts"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/users/me/posts")
                        .with(authentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                                userDetails(),
                                null,
                                userDetails().getAuthorities()
                        ))))
                .andExpect(status().isOk());

        verify(postQueryService).getMyPosts(eq(1L), any(Pageable.class));
    }

    @Test
    @DisplayName("내가 댓글 단 게시글 목록 API는 인증이 필요하고 인증 사용자는 접근할 수 있다.")
    void commentedPostsApi_requiresAuthenticationAndAllowsAuthenticatedUser() throws Exception {
        when(postQueryService.getCommentedPosts(anyLong(), any(Pageable.class)))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0, true));

        mockMvc.perform(get("/api/users/me/commented-posts"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/users/me/commented-posts")
                        .with(authentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                                userDetails(),
                                null,
                                userDetails().getAuthorities()
                        ))))
                .andExpect(status().isOk());

        verify(postQueryService).getCommentedPosts(eq(1L), any(Pageable.class));
    }

    @Test
    @DisplayName("신고 생성 API는 인증이 필요하고 인증 사용자는 호출할 수 있다.")
    void reportCreateApi_requiresAuthenticationAndAllowsAuthenticatedUser() throws Exception {
        when(reportService.create(eq(1L), any()))
                .thenReturn(reportResponse(1L));

        mockMvc.perform(post("/api/reports")
                        .contentType("application/json")
                        .content("""
                                {
                                  "targetType": "POST",
                                  "targetId": 10,
                                  "reason": "스팸",
                                  "content": "신고 상세 내용"
                                }
                                """))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/reports")
                        .contentType("application/json")
                        .content("""
                                {
                                  "targetType": "POST",
                                  "targetId": 10,
                                  "reason": "스팸",
                                  "content": "신고 상세 내용"
                                }
                                """)
                        .with(authentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                                userDetails(),
                                null,
                                userDetails().getAuthorities()
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.targetType").value(ReportTargetType.POST.name()))
                .andExpect(jsonPath("$.status").value(ReportStatus.PENDING.name()));
    }

    @Test
    @DisplayName("Admin Report API는 ADMIN 권한이 필요하고 일반 사용자는 접근할 수 없다.")
    void adminReportApis_requireAdminRole() throws Exception {
        when(reportService.findReports(any(Pageable.class)))
                .thenReturn(new PageResponse<>(List.of(reportResponse(1L)), 0, 20, 1, 1, true));
        when(reportService.findReport(1L)).thenReturn(reportResponse(1L));
        when(reportService.updateStatus(eq(1L), eq(1L), any(ReportStatusUpdateRequest.class)))
                .thenReturn(reportResponse(1L, ReportStatus.REVIEWED));
        when(adminModerationService.deleteTargetByReport(1L, 1L))
                .thenReturn(reportResponse(1L, ReportStatus.ACTION_TAKEN));
        when(adminModerationService.suspendUserByReport(
                eq(1L),
                eq(1L),
                eq(SuspensionType.TEMPORARY),
                eq(SuspensionReason.SPAM),
                any()
        )).thenReturn(reportResponse(1L, ReportStatus.ACTION_TAKEN));

        mockMvc.perform(get("/api/admin/reports"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/reports")
                        .with(authentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                                userDetails(),
                                null,
                                userDetails().getAuthorities()
                        ))))
                .andExpect(status().isForbidden());

        UserDetailsImpl adminDetails = userDetails("ADMIN");
        mockMvc.perform(get("/api/admin/reports")
                        .with(authentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                                adminDetails,
                                null,
                                adminDetails.getAuthorities()
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L));

        mockMvc.perform(get("/api/admin/reports/1")
                        .with(authentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                                adminDetails,
                                null,
                                adminDetails.getAuthorities()
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("신고 상세 내용"));

        mockMvc.perform(patch("/api/admin/reports/1/status")
                        .contentType("application/json")
                        .content("{\"status\":\"REVIEWED\"}")
                        .with(authentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                                adminDetails,
                                null,
                                adminDetails.getAuthorities()
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(ReportStatus.REVIEWED.name()));

        mockMvc.perform(post("/api/admin/reports/1/actions/delete-target"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/admin/reports/1/actions/delete-target")
                        .with(authentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                                userDetails(),
                                null,
                                userDetails().getAuthorities()
                        ))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/admin/reports/1/actions/delete-target")
                        .with(authentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                                adminDetails,
                                null,
                                adminDetails.getAuthorities()
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(ReportStatus.ACTION_TAKEN.name()));

        verify(adminModerationService).deleteTargetByReport(1L, 1L);

        mockMvc.perform(post("/api/admin/reports/1/actions/suspend-user"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/admin/reports/1/actions/suspend-user")
                        .contentType("application/json")
                        .content("""
                                {"type":"TEMPORARY","reason":"SPAM","days":7}
                                """)
                        .with(authentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                                userDetails(),
                                null,
                                userDetails().getAuthorities()
                        ))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/admin/reports/1/actions/suspend-user")
                        .contentType("application/json")
                        .content("""
                                {"type":"TEMPORARY","reason":"SPAM","days":7}
                                """)
                        .with(authentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                                adminDetails,
                                null,
                                adminDetails.getAuthorities()
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(ReportStatus.ACTION_TAKEN.name()));

        verify(adminModerationService).suspendUserByReport(
                eq(1L),
                eq(1L),
                eq(SuspensionType.TEMPORARY),
                eq(SuspensionReason.SPAM),
                any()
        );
    }

    @Test
    @DisplayName("Admin Moderation API는 ADMIN 권한이 필요하고 명시적 조치 서비스를 호출한다.")
    void adminModerationApis_requireAdminRole() throws Exception {
        mockMvc.perform(delete("/api/admin/moderation/posts/10"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/api/admin/moderation/posts/10")
                        .with(authentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                                userDetails(),
                                null,
                                userDetails().getAuthorities()
                        ))))
                .andExpect(status().isForbidden());

        UserDetailsImpl adminDetails = userDetails("ADMIN");
        mockMvc.perform(delete("/api/admin/moderation/posts/10")
                        .with(authentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                                adminDetails,
                                null,
                                adminDetails.getAuthorities()
                        ))))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/admin/moderation/comments/100")
                        .with(authentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                                adminDetails,
                                null,
                                adminDetails.getAuthorities()
                        ))))
                .andExpect(status().isNoContent());

        verify(adminModerationService).deletePost(10L);
        verify(adminModerationService).deleteComment(100L);
    }

    private UserDetailsImpl userDetails() {
        return userDetails("USER");
    }

    private UserDetailsImpl userDetails(String role) {
        return new UserDetailsImpl(
                1L,
                "user@example.com",
                null,
                role,
                AccountStatus.NORMAL,
                UserStatus.ACTIVE,
                null,
                null,
                null,
                null,
                false,
                false,
                Map.of()
        );
    }

    private OutboxEvent outboxEvent(Long id) {
        OutboxEvent event = OutboxEvent.create(
                OutboxEventType.USER_ACCOUNT_CHANGED,
                "{\"userId\":1}",
                "event-key",
                LocalDateTime.parse("2026-05-15T10:00:00")
        );
        ReflectionTestUtils.setField(event, "id", id);
        return event;
    }

    private ReportResponse reportResponse(Long id) {
        return reportResponse(id, ReportStatus.PENDING);
    }

    private ReportResponse reportResponse(Long id, ReportStatus status) {
        return new ReportResponse(
                id,
                ReportTargetType.POST,
                10L,
                1L,
                "스팸",
                "신고 상세 내용",
                status,
                LocalDateTime.parse("2026-05-15T10:00:00"),
                status == ReportStatus.PENDING ? null : LocalDateTime.parse("2026-05-15T11:00:00"),
                status == ReportStatus.PENDING ? null : 1L
        );
    }
}
