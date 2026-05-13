package com.example.my_project_1.post.service.impl;

import com.example.my_project_1.board.domain.Board;
import com.example.my_project_1.board.repository.BoardRepository;
import com.example.my_project_1.common.exception.CustomException;
import com.example.my_project_1.common.exception.ErrorCode;
import com.example.my_project_1.common.utils.DataSerializer;
import com.example.my_project_1.outbox.domain.OutboxEventType;
import com.example.my_project_1.outbox.service.OutboxPublisher;
import com.example.my_project_1.post.domain.Post;
import com.example.my_project_1.post.domain.PostLike;
import com.example.my_project_1.post.repository.PostLikeRepository;
import com.example.my_project_1.post.event.PostDeletedOutboxEvent;
import com.example.my_project_1.post.event.PostUpdatedOutboxEvent;
import com.example.my_project_1.post.repository.PostRepository;
import com.example.my_project_1.post.service.request.PostCreateRequest;
import com.example.my_project_1.post.service.request.PostUpdateRequest;
import com.example.my_project_1.post.service.response.PostDetailResponse;
import com.example.my_project_1.post.service.response.PostLikeResponse;
import com.example.my_project_1.user.client.AuthorStatus;
import com.example.my_project_1.user.client.AuthorSummary;
import com.example.my_project_1.user.client.UserClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

class PostCommandServiceImplTest {

    private static final String IMAGE_STORAGE_KEY = "123e4567-e89b-12d3-a456-426614174000.png";
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-05-06T01:02:03Z"),
            ZoneId.of("Asia/Seoul")
    );

    private BoardRepository boardRepository;
    private PostRepository postRepository;
    private PostLikeRepository postLikeRepository;
    private UserClient userClient;
    private OutboxPublisher outboxPublisher;
    private PostCommandServiceImpl postCommandService;

    @BeforeEach
    void setUp() {
        boardRepository = mock(BoardRepository.class);
        postRepository = mock(PostRepository.class);
        postLikeRepository = mock(PostLikeRepository.class);
        userClient = mock(UserClient.class);
        outboxPublisher = mock(OutboxPublisher.class);

        postCommandService = new PostCommandServiceImpl(
                boardRepository,
                postRepository,
                postLikeRepository,
                userClient,
                outboxPublisher,
                CLOCK
        );
    }

    @Test
    @DisplayName("post update??active post 議고쉶瑜??ъ슜?쒕떎.")
    void update_usesActivePostLookup() {
        Long boardId = 1L;
        Long postId = 10L;
        Long userId = 100L;
        Post post = post(boardId, postId, userId);
        PostUpdateRequest request = updateRequest("updated title", "updated content");

        when(postRepository.findActiveById(postId)).thenReturn(Optional.of(post));
        when(userClient.findAuthorsByIds(List.of(userId)))
                .thenReturn(Map.of(userId, AuthorSummary.active(userId, "nickname")));

        PostDetailResponse response = postCommandService.update(boardId, postId, userId, request);

        assertThat(response.getTitle()).isEqualTo("updated title");
        assertThat(response.getContent()).isEqualTo("updated content");
        assertThat(response.getNickname()).isEqualTo("nickname");
        assertThat(response.getAuthor().id()).isEqualTo(userId);
        assertThat(response.getAuthor().displayName()).isEqualTo("nickname");
        assertThat(response.getAuthor().status()).isEqualTo(AuthorStatus.ACTIVE);
        verify(postRepository).findActiveById(postId);
    }

    @Test
    @DisplayName("post update??uuid key濡?POST_UPDATED outbox event瑜?諛쒗뻾?쒕떎.")
    void update_publishesPostUpdatedOutboxEventWithUuidKey() {
        Long boardId = 1L;
        Long postId = 10L;
        Long userId = 100L;
        Post post = post(boardId, postId, userId);
        LocalDateTime oldUpdatedAt = LocalDateTime.of(2026, 1, 2, 3, 4, 5);
        ReflectionTestUtils.setField(post, "updatedAt", oldUpdatedAt);
        PostUpdateRequest request = updateRequest(
                "updated title",
                "updated content ![image](/images/%s) ![external](https://cdn.example.com/images/%s)"
                        .formatted(IMAGE_STORAGE_KEY, IMAGE_STORAGE_KEY)
        );

        when(postRepository.findActiveById(postId)).thenReturn(Optional.of(post));
        when(userClient.findAuthorsByIds(List.of(userId)))
                .thenReturn(Map.of(userId, AuthorSummary.active(userId, "nickname")));

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
        assertThat(payload.getStorageKeys()).containsExactly(IMAGE_STORAGE_KEY);
    }

    @Test
    @DisplayName("post create response includes AuthorSummary author")
    void create_returnsAuthorSummaryAuthor() {
        Long boardId = 1L;
        Long userId = 100L;
        Board board = board(boardId);
        PostCreateRequest request = createRequest("title", "content");

        when(boardRepository.findByIdAndDeletedAtIsNull(boardId)).thenReturn(Optional.of(board));
        when(userClient.findAuthorsByIds(List.of(userId)))
                .thenReturn(Map.of(userId, AuthorSummary.active(userId, "nickname")));

        PostDetailResponse response = postCommandService.create(boardId, userId, request);

        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getNickname()).isEqualTo("nickname");
        assertThat(response.getAuthor().id()).isEqualTo(userId);
        assertThat(response.getAuthor().displayName()).isEqualTo("nickname");
        assertThat(response.getAuthor().status()).isEqualTo(AuthorStatus.ACTIVE);
    }

    @Test
    @DisplayName("post create response uses UNKNOWN author when author lookup throws")
    void create_usesUnknownAuthorWhenUserLookupThrows() {
        Long boardId = 1L;
        Long userId = 100L;
        Board board = board(boardId);
        PostCreateRequest request = createRequest("title", "content");

        when(boardRepository.findByIdAndDeletedAtIsNull(boardId)).thenReturn(Optional.of(board));
        when(userClient.findAuthorsByIds(List.of(userId)))
                .thenThrow(new RuntimeException("user lookup failed"));

        PostDetailResponse response = postCommandService.create(boardId, userId, request);

        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getAuthor().id()).isNull();
        assertThat(response.getAuthor().status()).isEqualTo(AuthorStatus.UNKNOWN);
    }

    @Test
    @DisplayName("post update response uses UNKNOWN author when author lookup throws")
    void update_usesUnknownAuthorWhenUserLookupThrows() {
        Long boardId = 1L;
        Long postId = 10L;
        Long userId = 100L;
        Post post = post(boardId, postId, userId);
        PostUpdateRequest request = updateRequest("updated title", "updated content");

        when(postRepository.findActiveById(postId)).thenReturn(Optional.of(post));
        when(userClient.findAuthorsByIds(List.of(userId)))
                .thenThrow(new RuntimeException("user lookup failed"));

        PostDetailResponse response = postCommandService.update(boardId, postId, userId, request);

        assertThat(response.getTitle()).isEqualTo("updated title");
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getAuthor().id()).isNull();
        assertThat(response.getAuthor().status()).isEqualTo(AuthorStatus.UNKNOWN);
    }

    @Test
    @DisplayName("post update??active post瑜?李얠? 紐삵븯硫?嫄곕??쒕떎.")
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
    @DisplayName("post delete??active post瑜???젣?섍퀬 POST_DELETED outbox event瑜?諛쒗뻾?쒕떎.")
    void delete_deletesPostAndPublishesPostDeletedOutboxEvent() {
        Long boardId = 1L;
        Long postId = 10L;
        Long userId = 100L;
        Post post = post(boardId, postId, userId);

        when(postRepository.findActiveById(postId)).thenReturn(Optional.of(post));

        postCommandService.delete(boardId, postId, userId);

        ArgumentCaptor<OutboxEventType> typeCaptor = ArgumentCaptor.forClass(OutboxEventType.class);
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> eventKeyCaptor = ArgumentCaptor.forClass(String.class);

        verify(postRepository).findActiveById(postId);
        verify(outboxPublisher).publish(
                typeCaptor.capture(),
                payloadCaptor.capture(),
                eventKeyCaptor.capture()
        );

        String eventKey = eventKeyCaptor.getValue();
        String[] eventKeyParts = eventKey.split(":");
        PostDeletedOutboxEvent payload =
                DataSerializer.deserialize(payloadCaptor.getValue(), PostDeletedOutboxEvent.class);

        assertThat(post.getDeletedAt()).isEqualTo(LocalDateTime.now(CLOCK));
        assertThat(typeCaptor.getValue()).isEqualTo(OutboxEventType.POST_DELETED);
        assertThat(eventKeyParts).hasSize(2);
        assertThat(eventKeyParts[0]).isEqualTo("POST_DELETED");
        assertThat(eventKeyParts[1]).isEqualTo(postId.toString());
        assertThat(payload.getPostId()).isEqualTo(postId);
        assertThat(payload.getUserId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("post delete??active post瑜?李얠? 紐삵븯硫?POST_NOT_FOUND濡?嫄곗젅?쒕떎.")
    void delete_rejectsWhenActivePostNotFound() {
        Long boardId = 1L;
        Long postId = 10L;
        Long userId = 100L;

        when(postRepository.findActiveById(postId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postCommandService.delete(boardId, postId, userId))
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
    @DisplayName("post delete????젣??board ?꾨옒 post??POST_NOT_FOUND濡?嫄곗젅?쒕떎.")
    void delete_rejectsPostUnderDeletedBoardAsNotFound() {
        Long boardId = 1L;
        Long postId = 10L;
        Long userId = 100L;

        when(postRepository.findActiveById(postId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postCommandService.delete(boardId, postId, userId))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.POST_NOT_FOUND);
    }

    @Test
    @DisplayName("post delete??board-post 愿怨꾧? ?ㅻⅤ硫?INVALID_BOARD_POST_RELATION?쇰줈 嫄곗젅?쒕떎.")
    void delete_rejectsWhenBoardPostRelationMismatch() {
        Long boardId = 1L;
        Long actualBoardId = 2L;
        Long postId = 10L;
        Long userId = 100L;
        Post post = post(actualBoardId, postId, userId);

        when(postRepository.findActiveById(postId)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> postCommandService.delete(boardId, postId, userId))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_BOARD_POST_RELATION);

        verify(outboxPublisher, never()).publish(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString()
        );
    }

    @Test
    @DisplayName("post delete???묒꽦?먭? ?꾨땲硫?ACCESS_DENIED濡?嫄곗젅?쒕떎.")
    void delete_rejectsWhenUserIsNotAuthor() {
        Long boardId = 1L;
        Long postId = 10L;
        Long authorId = 100L;
        Long requesterId = 200L;
        Post post = post(boardId, postId, authorId);

        when(postRepository.findActiveById(postId)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> postCommandService.delete(boardId, postId, requesterId))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ACCESS_DENIED);

        verify(outboxPublisher, never()).publish(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString()
        );
    }


    @Test
    @DisplayName("PUT like inserts post_like and increments likeCount when row does not exist")
    void likeIdempotently_insertsPostLikeAndIncrementsLikeCountWhenRowDoesNotExist() {
        Long boardId = 1L;
        Long postId = 10L;
        Long userId = 100L;
        Post post = post(boardId, postId, userId);

        when(postRepository.findActiveById(postId)).thenReturn(Optional.of(post));
        when(postRepository.updateLikeCountDelta(postId, 1L)).thenReturn(1);
        when(postRepository.findLikeCountById(postId)).thenReturn(Optional.of(8L));

        PostLikeResponse response = postCommandService.likeIdempotently(boardId, postId, userId);

        assertThat(response.isLiked()).isTrue();
        assertThat(response.getLikeCount()).isEqualTo(8L);
        verify(postRepository).findActiveById(postId);
        verify(postLikeRepository).saveAndFlush(any(PostLike.class));
        verify(postRepository).updateLikeCountDelta(postId, 1L);
    }

    @Test
    @DisplayName("PUT like keeps likeCount when unique constraint reports already liked")
    void likeIdempotently_keepsLikeCountWhenUniqueConstraintConflicts() {
        Long boardId = 1L;
        Long postId = 10L;
        Long userId = 100L;
        Post post = post(boardId, postId, userId);

        when(postRepository.findActiveById(postId)).thenReturn(Optional.of(post));
        doThrow(new DataIntegrityViolationException("duplicate post_like"))
                .when(postLikeRepository).saveAndFlush(any(PostLike.class));
        when(postRepository.findLikeCountById(postId)).thenReturn(Optional.of(7L));

        PostLikeResponse response = postCommandService.likeIdempotently(boardId, postId, userId);

        assertThat(response.isLiked()).isTrue();
        assertThat(response.getLikeCount()).isEqualTo(7L);
        verify(postLikeRepository).saveAndFlush(any(PostLike.class));
        verify(postRepository, never()).updateLikeCountDelta(postId, 1L);
    }

    @Test
    @DisplayName("DELETE like deletes post_like and decrements likeCount when row exists")
    void unlikeIdempotently_deletesPostLikeAndDecrementsLikeCountWhenRowExists() {
        Long boardId = 1L;
        Long postId = 10L;
        Long userId = 100L;
        Post post = post(boardId, postId, userId);

        when(postRepository.findActiveById(postId)).thenReturn(Optional.of(post));
        when(postLikeRepository.deleteByPostIdAndUserId(postId, userId)).thenReturn(1);
        when(postRepository.updateLikeCountDelta(postId, -1L)).thenReturn(1);
        when(postRepository.findLikeCountById(postId)).thenReturn(Optional.of(6L));

        PostLikeResponse response = postCommandService.unlikeIdempotently(boardId, postId, userId);

        assertThat(response.isLiked()).isFalse();
        assertThat(response.getLikeCount()).isEqualTo(6L);
        verify(postLikeRepository).deleteByPostIdAndUserId(postId, userId);
        verify(postRepository).updateLikeCountDelta(postId, -1L);
    }

    @Test
    @DisplayName("DELETE like keeps likeCount when row does not exist")
    void unlikeIdempotently_keepsLikeCountWhenRowDoesNotExist() {
        Long boardId = 1L;
        Long postId = 10L;
        Long userId = 100L;
        Post post = post(boardId, postId, userId);

        when(postRepository.findActiveById(postId)).thenReturn(Optional.of(post));
        when(postLikeRepository.deleteByPostIdAndUserId(postId, userId)).thenReturn(0);
        when(postRepository.findLikeCountById(postId)).thenReturn(Optional.of(7L));

        PostLikeResponse response = postCommandService.unlikeIdempotently(boardId, postId, userId);

        assertThat(response.isLiked()).isFalse();
        assertThat(response.getLikeCount()).isEqualTo(7L);
        verify(postLikeRepository).deleteByPostIdAndUserId(postId, userId);
        verify(postRepository, never()).updateLikeCountDelta(postId, -1L);
    }

    @Test
    @DisplayName("PUT like rejects mismatched board-post relation")
    void likeIdempotently_rejectsWhenBoardPostRelationMismatch() {
        Long boardId = 1L;
        Long actualBoardId = 2L;
        Long postId = 10L;
        Long userId = 100L;
        Post post = post(actualBoardId, postId, userId);

        when(postRepository.findActiveById(postId)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> postCommandService.likeIdempotently(boardId, postId, userId))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_BOARD_POST_RELATION);

        verify(postLikeRepository, never()).saveAndFlush(any(PostLike.class));
    }

    @Test
    @DisplayName("DELETE like rejects when active post is not found")
    void unlikeIdempotently_rejectsWhenActivePostNotFound() {
        Long boardId = 1L;
        Long postId = 10L;
        Long userId = 100L;

        when(postRepository.findActiveById(postId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postCommandService.unlikeIdempotently(boardId, postId, userId))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.POST_NOT_FOUND);

        verify(postLikeRepository, never()).deleteByPostIdAndUserId(postId, userId);
    }
    private static Post post(Long boardId, Long postId, Long userId) {
        Board board = board(boardId);
        Post post = Post.create(board, userId, "title", "content");
        ReflectionTestUtils.setField(post, "id", postId);
        return post;
    }

    private static Board board(Long boardId) {
        Board board = Board.create("board", "description");
        ReflectionTestUtils.setField(board, "id", boardId);
        return board;
    }

    private static PostCreateRequest createRequest(String title, String content) {
        PostCreateRequest request = new PostCreateRequest();
        ReflectionTestUtils.setField(request, "title", title);
        ReflectionTestUtils.setField(request, "content", content);
        return request;
    }

    private static PostUpdateRequest updateRequest(String title, String content) {
        PostUpdateRequest request = new PostUpdateRequest();
        ReflectionTestUtils.setField(request, "title", title);
        ReflectionTestUtils.setField(request, "content", content);
        return request;
    }
}
