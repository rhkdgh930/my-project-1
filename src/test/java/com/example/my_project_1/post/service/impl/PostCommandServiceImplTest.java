package com.example.my_project_1.post.service.impl;

import com.example.my_project_1.board.domain.Board;
import com.example.my_project_1.board.repository.BoardRepository;
import com.example.my_project_1.common.exception.CustomException;
import com.example.my_project_1.common.exception.ErrorCode;
import com.example.my_project_1.outbox.service.OutboxPublisher;
import com.example.my_project_1.post.domain.Post;
import com.example.my_project_1.post.repository.PostRepository;
import com.example.my_project_1.post.service.PostRedisService;
import com.example.my_project_1.post.service.request.PostUpdateRequest;
import com.example.my_project_1.post.service.response.PostDetailResponse;
import com.example.my_project_1.user.client.UserClient;
import com.example.my_project_1.user.client.UserSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PostCommandServiceImplTest {

    private BoardRepository boardRepository;
    private PostRepository postRepository;
    private PostRedisService postRedisService;
    private UserClient userClient;
    private OutboxPublisher outboxPublisher;
    private PostCommandServiceImpl postCommandService;

    @BeforeEach
    void setUp() {
        boardRepository = mock(BoardRepository.class);
        postRepository = mock(PostRepository.class);
        postRedisService = mock(PostRedisService.class);
        userClient = mock(UserClient.class);
        outboxPublisher = mock(OutboxPublisher.class);

        postCommandService = new PostCommandServiceImpl(
                boardRepository,
                postRepository,
                postRedisService,
                userClient,
                outboxPublisher
        );
    }

    @Test
    @DisplayName("게시글 수정은 게시글과 게시판이 모두 삭제되지 않은 active post만 대상으로 한다.")
    void update_usesActivePostLookup() {
        Long boardId = 1L;
        Long postId = 10L;
        Long userId = 100L;
        Post post = post(boardId, postId, userId);
        PostUpdateRequest request = updateRequest("updated title", "updated content");

        when(postRepository.findActiveById(postId)).thenReturn(Optional.of(post));
        when(userClient.findUsersByIds(List.of(userId)))
                .thenReturn(Map.of(userId, new UserSummary(userId, "nickname")));

        PostDetailResponse response = postCommandService.update(boardId, postId, userId, request);

        assertThat(response.getTitle()).isEqualTo("updated title");
        assertThat(response.getContent()).isEqualTo("updated content");
        verify(postRepository).findActiveById(postId);
    }

    @Test
    @DisplayName("active post가 아니면 게시글 수정은 POST_NOT_FOUND로 거부된다.")
    void update_rejectsWhenActivePostNotFound() {
        Long boardId = 1L;
        Long postId = 10L;
        Long userId = 100L;
        PostUpdateRequest request = updateRequest("updated title", "updated content");
        when(postRepository.findActiveById(postId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postCommandService.update(boardId, postId, userId, request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.POST_NOT_FOUND);

        verify(postRepository).findActiveById(postId);
        verify(outboxPublisher, never()).publish(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    @DisplayName("좋아요는 게시글과 게시판이 모두 삭제되지 않은 active post만 대상으로 한다.")
    void like_usesActivePostLookup() {
        Long boardId = 1L;
        Long postId = 10L;
        Long userId = 100L;
        Post post = post(boardId, postId, userId);

        when(postRepository.findActiveById(postId)).thenReturn(Optional.of(post));
        when(postRedisService.toggleLike(postId, userId)).thenReturn(true);

        boolean liked = postCommandService.like(boardId, postId, userId);

        assertThat(liked).isTrue();
        verify(postRepository).findActiveById(postId);
    }

    private static Post post(Long boardId, Long postId, Long userId) {
        Board board = Board.create("board", "description");
        ReflectionTestUtils.setField(board, "id", boardId);
        Post post = Post.create(board, userId, "title", "content");
        ReflectionTestUtils.setField(post, "id", postId);
        return post;
    }

    private static PostUpdateRequest updateRequest(String title, String content) {
        PostUpdateRequest request = new PostUpdateRequest();
        ReflectionTestUtils.setField(request, "title", title);
        ReflectionTestUtils.setField(request, "content", content);
        return request;
    }
}
