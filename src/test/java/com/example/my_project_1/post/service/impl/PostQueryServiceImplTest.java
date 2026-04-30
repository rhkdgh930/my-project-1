package com.example.my_project_1.post.service.impl;

import com.example.my_project_1.board.domain.Board;
import com.example.my_project_1.common.exception.CustomException;
import com.example.my_project_1.common.exception.ErrorCode;
import com.example.my_project_1.common.utils.PageResponse;
import com.example.my_project_1.post.domain.Post;
import com.example.my_project_1.post.repository.PostRepository;
import com.example.my_project_1.post.service.PostRedisService;
import com.example.my_project_1.post.service.response.PostDetailResponse;
import com.example.my_project_1.post.service.response.PostListResponse;
import com.example.my_project_1.user.client.UserClient;
import com.example.my_project_1.user.client.UserSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PostQueryServiceImplTest {

    private PostRepository postRepository;
    private UserClient userClient;
    private PostRedisService postRedisService;
    private PostQueryServiceImpl postQueryService;

    @BeforeEach
    void setUp() {
        postRepository = mock(PostRepository.class);
        userClient = mock(UserClient.class);
        postRedisService = mock(PostRedisService.class);
        postQueryService = new PostQueryServiceImpl(postRepository, userClient, postRedisService);
    }

    @Test
    @DisplayName("게시글 목록 조회는 게시글과 게시판이 모두 삭제되지 않은 active post만 조회한다.")
    void getPosts_usesActiveBoardPostQuery() {
        Long boardId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        Post post = post(boardId, 10L, 100L);

        when(postRepository.findAllActiveByBoardId(boardId, pageable))
                .thenReturn(new PageImpl<>(List.of(post), pageable, 1));
        when(userClient.findUsersByIds(List.of(100L)))
                .thenReturn(Map.of(100L, new UserSummary(100L, "nickname")));
        when(postRedisService.getView(10L)).thenReturn(3L);
        when(postRedisService.getLike(10L)).thenReturn(2L);

        PageResponse<PostListResponse> response = postQueryService.getPosts(boardId, pageable);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getPostId()).isEqualTo(10L);
        verify(postRepository).findAllActiveByBoardId(boardId, pageable);
    }

    @Test
    @DisplayName("게시글 상세 조회는 active post 확인 후 조회수를 증가시킨다.")
    void getPostDetail_increasesViewAfterActivePostFound() {
        Long boardId = 1L;
        Long postId = 10L;
        Post post = post(boardId, postId, 100L);

        when(postRepository.findActiveById(postId)).thenReturn(Optional.of(post));
        when(userClient.findUsersByIds(List.of(100L)))
                .thenReturn(Map.of(100L, new UserSummary(100L, "nickname")));
        when(postRedisService.getView(postId)).thenReturn(4L);
        when(postRedisService.getLike(postId)).thenReturn(2L);

        PostDetailResponse response = postQueryService.getPostDetail(boardId, postId);

        assertThat(response.getPostId()).isEqualTo(postId);
        assertThat(response.getViewCount()).isEqualTo(4L);
        InOrder inOrder = inOrder(postRepository, postRedisService);
        inOrder.verify(postRepository).findActiveById(postId);
        inOrder.verify(postRedisService).increaseView(postId);
    }

    @Test
    @DisplayName("active post가 아니면 상세 조회수 증가 없이 POST_NOT_FOUND를 던진다.")
    void getPostDetail_doesNotIncreaseViewWhenActivePostNotFound() {
        Long boardId = 1L;
        Long postId = 10L;
        when(postRepository.findActiveById(postId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postQueryService.getPostDetail(boardId, postId))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.POST_NOT_FOUND);

        verify(postRedisService, never()).increaseView(postId);
    }

    private static Post post(Long boardId, Long postId, Long userId) {
        Board board = Board.create("board", "description");
        ReflectionTestUtils.setField(board, "id", boardId);
        Post post = Post.create(board, userId, "title", "content");
        ReflectionTestUtils.setField(post, "id", postId);
        return post;
    }
}
