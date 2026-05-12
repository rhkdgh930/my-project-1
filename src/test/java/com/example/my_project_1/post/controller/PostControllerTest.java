package com.example.my_project_1.post.controller;

import com.example.my_project_1.auth.userdetails.UserDetailsImpl;
import com.example.my_project_1.post.service.PostCommandService;
import com.example.my_project_1.post.service.PostQueryService;
import com.example.my_project_1.post.service.response.PostLikeResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
}
