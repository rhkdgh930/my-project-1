package com.example.my_project_1.post.service.impl;

import com.example.my_project_1.board.domain.Board;
import com.example.my_project_1.board.repository.BoardRepository;
import com.example.my_project_1.common.exception.CustomException;
import com.example.my_project_1.common.exception.ErrorCode;
import com.example.my_project_1.common.utils.DataSerializer;
import com.example.my_project_1.outbox.domain.OutboxEventType;
import com.example.my_project_1.outbox.service.OutboxPublisher;
import com.example.my_project_1.post.domain.Post;
import com.example.my_project_1.post.event.PostUpdatedOutboxEvent;
import com.example.my_project_1.post.repository.PostRepository;
import com.example.my_project_1.post.service.PostRedisService;
import com.example.my_project_1.post.service.request.PostUpdateRequest;
import com.example.my_project_1.post.service.response.PostDetailResponse;
import com.example.my_project_1.user.client.UserClient;
import com.example.my_project_1.user.client.UserSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
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
    @DisplayName("post update uses active post lookup")
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
    @DisplayName("post update publishes POST_UPDATED outbox event with uuid key")
    void update_publishesPostUpdatedOutboxEventWithUuidKey() {
        Long boardId = 1L;
        Long postId = 10L;
        Long userId = 100L;
        Post post = post(boardId, postId, userId);
        LocalDateTime oldUpdatedAt = LocalDateTime.of(2026, 1, 2, 3, 4, 5);
        ReflectionTestUtils.setField(post, "updatedAt", oldUpdatedAt);
        PostUpdateRequest request = updateRequest(
                "updated title",
                "updated content ![image](/images/storage-key-1)"
        );

        when(postRepository.findActiveById(postId)).thenReturn(Optional.of(post));
        when(userClient.findUsersByIds(List.of(userId)))
                .thenReturn(Map.of(userId, new UserSummary(userId, "nickname")));

        postCommandService.update(boardId, postId, userId, request);

        ArgumentCaptor<OutboxEventType> typeCaptor = ArgumentCaptor.forClass(OutboxEventType.class);
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> eventKeyCaptor = ArgumentCaptor.forClass(String.class);

        verify(outboxPublisher).publish(
                typeCaptor.capture(),
                payloadCaptor.capture(),
                eventKeyCaptor.capture()
        );

        String eventKey = eventKeyCaptor.getValue();
        String[] eventKeyParts = eventKey.split(":");
        PostUpdatedOutboxEvent payload =
                DataSerializer.deserialize(payloadCaptor.getValue(), PostUpdatedOutboxEvent.class);

        assertThat(typeCaptor.getValue()).isEqualTo(OutboxEventType.POST_UPDATED);
        assertThat(eventKeyParts).hasSize(3);
        assertThat(eventKeyParts[0]).isEqualTo("POST_UPDATED");
        assertThat(eventKeyParts[1]).isEqualTo(postId.toString());
        assertThatCode(() -> java.util.UUID.fromString(eventKeyParts[2]))
                .doesNotThrowAnyException();
        assertThat(eventKey).doesNotContain(oldUpdatedAt.toString());

        assertThat(payload.getPostId()).isEqualTo(postId);
        assertThat(payload.getUserId()).isEqualTo(userId);
        assertThat(payload.getStorageKeys()).containsExactly("storage-key-1");
    }

    @Test
    @DisplayName("post update rejects when active post is not found")
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
        verify(outboxPublisher, never()).publish(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString()
        );
    }

    @Test
    @DisplayName("like uses active post lookup")
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
