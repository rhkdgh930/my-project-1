package com.example.my_project_1.post.controller;

import com.example.my_project_1.auth.userdetails.UserDetailsImpl;
import com.example.my_project_1.common.utils.PageResponse;
import com.example.my_project_1.post.service.PostCommandService;
import com.example.my_project_1.post.service.PostQueryService;
import com.example.my_project_1.post.service.request.PostSearchCondition;
import com.example.my_project_1.post.service.request.PostSearchType;
import com.example.my_project_1.post.service.request.PostSortType;
import com.example.my_project_1.post.service.response.PostDetailResponse;
import com.example.my_project_1.post.service.response.PostLikeResponse;
import com.example.my_project_1.post.service.response.PostListResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PostControllerTest {

    private final PostCommandService postCommandService = mock(PostCommandService.class);
    private final PostQueryService postQueryService = mock(PostQueryService.class);
    private final PostController postController = new PostController(postCommandService, postQueryService);

    @Test
    @DisplayName("POST like endpoint is not exposed by controller")
    void postLikeEndpoint_isRemoved() {
        assertThat(List.of(PostController.class.getDeclaredMethods()))
                .noneMatch(method -> method.isAnnotationPresent(org.springframework.web.bind.annotation.PostMapping.class)
                        && method.getName().toLowerCase().contains("like"));
    }

    @Test
    @DisplayName("PUT like returns liked state and current like count")
    void likeIdempotently_returnsPostLikeResponse() {
        Long boardId = 1L;
        Long postId = 10L;
        Long userId = 100L;
        UserDetailsImpl userDetails = mock(UserDetailsImpl.class);
        PostLikeResponse response = PostLikeResponse.of(true, 12L);

        when(userDetails.getUserId()).thenReturn(userId);
        when(postCommandService.likeIdempotently(boardId, postId, userId)).thenReturn(response);

        PostLikeResponse actual = postController.likeIdempotently(boardId, postId, userDetails);

        assertThat(actual.isLiked()).isTrue();
        assertThat(actual.getLikeCount()).isEqualTo(12L);
        verify(postCommandService).likeIdempotently(boardId, postId, userId);
    }
    @Test
    @DisplayName("DELETE like returns unliked state and current like count")
    void unlikeIdempotently_returnsPostLikeResponse() {
        Long boardId = 1L;
        Long postId = 10L;
        Long userId = 100L;
        UserDetailsImpl userDetails = mock(UserDetailsImpl.class);
        PostLikeResponse response = PostLikeResponse.of(false, 11L);

        when(userDetails.getUserId()).thenReturn(userId);
        when(postCommandService.unlikeIdempotently(boardId, postId, userId)).thenReturn(response);

        PostLikeResponse actual = postController.unlikeIdempotently(boardId, postId, userDetails);

        assertThat(actual.isLiked()).isFalse();
        assertThat(actual.getLikeCount()).isEqualTo(11L);
        verify(postCommandService).unlikeIdempotently(boardId, postId, userId);
    }

    @Test
    @DisplayName("read passes nullable current user id to query service")
    void read_passesNullableCurrentUserId() {
        Long boardId = 1L;
        Long postId = 10L;
        PostDetailResponse response = mock(PostDetailResponse.class);

        when(postQueryService.getPostDetail(boardId, postId, null)).thenReturn(response);

        PostDetailResponse actual = postController.read(boardId, postId, null);

        assertThat(actual).isSameAs(response);
        verify(postQueryService).getPostDetail(boardId, postId, null);
    }

    @Test
    @DisplayName("read passes authenticated current user id to query service")
    void read_passesAuthenticatedCurrentUserId() {
        Long boardId = 1L;
        Long postId = 10L;
        Long userId = 100L;
        UserDetailsImpl userDetails = mock(UserDetailsImpl.class);
        PostDetailResponse response = mock(PostDetailResponse.class);

        when(userDetails.getUserId()).thenReturn(userId);
        when(postQueryService.getPostDetail(boardId, postId, userId)).thenReturn(response);

        PostDetailResponse actual = postController.read(boardId, postId, userDetails);

        assertThat(actual).isSameAs(response);
        verify(postQueryService).getPostDetail(boardId, postId, userId);
    }

    @Test
    @DisplayName("getPosts passes search condition and pageable to query service")
    void getPosts_passesSearchConditionAndPageable() {
        Long boardId = 1L;
        PostSearchCondition condition = new PostSearchCondition();
        condition.setKeyword("redis");
        condition.setSearchType(PostSearchType.TITLE_CONTENT);
        condition.setSortType(PostSortType.LATEST);
        Pageable pageable = PageRequest.of(0, 20);
        PageResponse<PostListResponse> response = PageResponse.of(new PageImpl<>(List.of(), pageable, 0));

        when(postQueryService.getPosts(boardId, condition, pageable)).thenReturn(response);

        PageResponse<PostListResponse> actual = postController.getPosts(boardId, condition, pageable);

        assertThat(actual.getContent()).isEmpty();
        verify(postQueryService).getPosts(boardId, condition, pageable);
    }
}
