package com.example.my_project_1.post.controller;

import com.example.my_project_1.auth.userdetails.UserDetailsImpl;
import com.example.my_project_1.common.utils.PageResponse;
import com.example.my_project_1.post.service.PostCommandService;
import com.example.my_project_1.post.service.PostQueryService;
import com.example.my_project_1.post.service.request.PostSearchCondition;
import com.example.my_project_1.post.service.request.PostSearchType;
import com.example.my_project_1.post.service.request.PostSortType;
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
    @DisplayName("like returns liked state and current like count")
    void like_returnsPostLikeResponse() {
        Long boardId = 1L;
        Long postId = 10L;
        Long userId = 100L;
        UserDetailsImpl userDetails = mock(UserDetailsImpl.class);
        PostLikeResponse response = PostLikeResponse.of(true, 12L);

        when(userDetails.getUserId()).thenReturn(userId);
        when(postCommandService.like(boardId, postId, userId)).thenReturn(response);

        PostLikeResponse actual = postController.like(boardId, postId, userDetails);

        assertThat(actual.isLiked()).isTrue();
        assertThat(actual.getLikeCount()).isEqualTo(12L);
        verify(postCommandService).like(boardId, postId, userId);
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
