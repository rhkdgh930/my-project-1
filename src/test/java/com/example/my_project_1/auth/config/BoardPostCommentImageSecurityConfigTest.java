package com.example.my_project_1.auth.config;

import com.example.my_project_1.auth.filter.JwtAuthenticationFilter;
import com.example.my_project_1.auth.handler.JwtAccessDeniedHandler;
import com.example.my_project_1.auth.handler.JwtAuthenticationEntryPoint;
import com.example.my_project_1.auth.handler.JwtLoginFailureHandler;
import com.example.my_project_1.auth.handler.JwtLoginSuccessHandler;
import com.example.my_project_1.auth.handler.OAuth2LoginSuccessHandler;
import com.example.my_project_1.auth.oauth.CustomOAuth2UserService;
import com.example.my_project_1.auth.service.RedisLoginAttemptService;
import com.example.my_project_1.auth.service.RedisTokenService;
import com.example.my_project_1.auth.service.RedisUserContextService;
import com.example.my_project_1.auth.utils.JwtProvider;
import com.example.my_project_1.board.controller.AdminBoardController;
import com.example.my_project_1.board.controller.BoardController;
import com.example.my_project_1.board.service.BoardCommandService;
import com.example.my_project_1.board.service.BoardQueryService;
import com.example.my_project_1.comment.controller.CommentController;
import com.example.my_project_1.comment.service.CommentCommandService;
import com.example.my_project_1.comment.service.CommentQueryService;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        BoardController.class,
        AdminBoardController.class,
        PostController.class,
        CommentController.class,
        ImageController.class
})
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtAuthenticationEntryPoint.class,
        JwtAccessDeniedHandler.class,
        TraceIdFilter.class,
        SecurityUserMdcFilter.class,
        HttpLoggingFilter.class
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
}
