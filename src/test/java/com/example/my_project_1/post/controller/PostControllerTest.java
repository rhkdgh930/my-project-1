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
    @DisplayName("POST /like endpoint는 controller에 노출되지 않는다.")
    void postLikeEndpoint_isRemoved() {
        assertThat(List.of(PostController.class.getDeclaredMethods()))
                .noneMatch(method -> method.isAnnotationPresent(org.springframework.web.bind.annotation.PostMapping.class)
                        && method.getName().toLowerCase().contains("like"));
    }

    @Test
    @DisplayName("PUT /like는 좋아요 상태와 현재 likeCount를 반환한다.")
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
    @DisplayName("DELETE /like는 좋아요 취소 상태와 현재 likeCount를 반환한다.")
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
    @DisplayName("상세 조회는 nullable current user id를 query service에 전달한다.")
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
    @DisplayName("상세 조회는 인증된 current user id를 query service에 전달한다.")
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
    @DisplayName("게시글 목록 조회는 검색 조건과 pageable을 query service에 전달한다.")
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
