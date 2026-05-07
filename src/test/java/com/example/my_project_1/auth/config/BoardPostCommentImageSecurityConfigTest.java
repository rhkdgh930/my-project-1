package com.example.my_project_1.auth.config;

import com.example.my_project_1.auth.filter.JwtAuthenticationFilter;
import com.example.my_project_1.auth.handler.JwtAccessDeniedHandler;
import com.example.my_project_1.auth.handler.JwtAuthenticationEntryPoint;
import com.example.my_project_1.auth.handler.JwtLoginFailureHandler;
import com.example.my_project_1.auth.handler.JwtLoginSuccessHandler;
import com.example.my_project_1.auth.handler.OAuth2LoginSuccessHandler;
import com.example.my_project_1.auth.controller.AuthController;
import com.example.my_project_1.auth.oauth.CustomOAuth2UserService;
import com.example.my_project_1.auth.service.AuthService;
import com.example.my_project_1.auth.service.RedisLoginAttemptService;
import com.example.my_project_1.auth.service.RedisTokenService;
import com.example.my_project_1.auth.service.RedisUserContextService;
import com.example.my_project_1.auth.service.response.TokenResponse;
import com.example.my_project_1.auth.userdetails.UserDetailsImpl;
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
import com.example.my_project_1.common.logging.HttpLoggingFilter;
import com.example.my_project_1.common.logging.SecurityUserMdcFilter;
import com.example.my_project_1.common.logging.TraceIdFilter;
import com.example.my_project_1.common.utils.PageResponse;
import com.example.my_project_1.image.controller.ImageController;
import com.example.my_project_1.image.service.ImageStorage;
import com.example.my_project_1.image.service.ImageUploadService;
import com.example.my_project_1.post.controller.PostController;
import com.example.my_project_1.post.service.PostCommandService;
import com.example.my_project_1.post.service.PostQueryService;
import com.example.my_project_1.user.controller.UserController;
import com.example.my_project_1.user.domain.AccountStatus;
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

import java.util.List;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        BoardController.class,
        AdminBoardController.class,
        PostController.class,
        CommentController.class,
        ImageController.class,
        AuthController.class,
        UserController.class
})
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtAuthenticationEntryPoint.class,
        JwtAccessDeniedHandler.class,
        TraceIdFilter.class,
        SecurityUserMdcFilter.class,
        HttpLoggingFilter.class,
        PasswordConfig.class
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
    private CustomOAuth2UserService customOAuth2UserService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Board, Post, Comment 조회 API는 인증 없이 접근할 수 있다.")
    void readApis_arePermitAll() throws Exception {
        when(boardQueryService.findAllBoards()).thenReturn(List.of());
        when(postQueryService.getPosts(eq(1L), any(Pageable.class)))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0, true));
        when(commentQueryService.getComments(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/boards"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/boards/1"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/boards/1/posts"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/boards/1/posts/1"))
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
        mockMvc.perform(post("/api/boards/1/posts/1/like"))
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

    private UserDetailsImpl userDetails() {
        return new UserDetailsImpl(
                1L,
                "user@example.com",
                null,
                "USER",
                AccountStatus.NORMAL,
                UserStatus.ACTIVE,
                null,
                null,
                null,
                null,
                false,
                false
        );
    }
}
