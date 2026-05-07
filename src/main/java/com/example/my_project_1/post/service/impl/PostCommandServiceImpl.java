package com.example.my_project_1.post.service.impl;

import com.example.my_project_1.board.domain.Board;
import com.example.my_project_1.board.repository.BoardRepository;
import com.example.my_project_1.common.exception.CustomException;
import com.example.my_project_1.common.exception.ErrorCode;
import com.example.my_project_1.common.utils.DataSerializer;
import com.example.my_project_1.image.utils.ImageUrlParser;
import com.example.my_project_1.outbox.domain.OutboxEventKey;
import com.example.my_project_1.outbox.domain.OutboxEventType;
import com.example.my_project_1.outbox.service.OutboxPublisher;
import com.example.my_project_1.post.domain.Post;
import com.example.my_project_1.post.event.PostCreatedOutboxEvent;
import com.example.my_project_1.post.event.PostDeletedOutboxEvent;
import com.example.my_project_1.post.event.PostUpdatedOutboxEvent;
import com.example.my_project_1.post.repository.PostRepository;
import com.example.my_project_1.post.service.PostCommandService;
import com.example.my_project_1.post.service.PostRedisService;
import com.example.my_project_1.post.service.request.PostCreateRequest;
import com.example.my_project_1.post.service.request.PostUpdateRequest;
import com.example.my_project_1.post.service.response.PostDetailResponse;
import com.example.my_project_1.user.client.AuthorSummary;
import com.example.my_project_1.user.client.UserClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Transactional
@RequiredArgsConstructor
@Slf4j
@Service
public class PostCommandServiceImpl implements PostCommandService {

    private final BoardRepository boardRepository;
    private final PostRepository postRepository;
    private final PostRedisService postRedisService;
    private final UserClient userClient;
    private final OutboxPublisher outboxPublisher;
    private final Clock clock;

    @Override
    public PostDetailResponse create(Long boardId, Long userId, PostCreateRequest request) {
        Board board = boardRepository.findByIdAndDeletedAtIsNull(boardId)
                .orElseThrow(() -> new CustomException(ErrorCode.BOARD_NOT_FOUND));

        Post post = Post.create(
                board,
                userId,
                request.getTitle(),
                request.getContent()
        );

        postRepository.save(post);

        List<String> keys = ImageUrlParser.extractStorageKeys(request.getContent());

        outboxPublisher.publish(
                OutboxEventType.POST_CREATED,
                DataSerializer.serialize(
                        new PostCreatedOutboxEvent(post.getId(), userId, keys)
                ),
                OutboxEventKey.postCreated(post.getId())
        );

        return PostDetailResponse.from(post, getAuthorOrUnknown(userId));
    }

    @Override
    public PostDetailResponse update(Long boardId, Long postId, Long userId, PostUpdateRequest request) {

        Post post = postRepository.findActiveById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        if (!post.getBoard().getId().equals(boardId)) {
            throw new CustomException(ErrorCode.INVALID_BOARD_POST_RELATION);
        }

        if (!post.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        post.update(request.getTitle(), request.getContent());

        List<String> keys =
                ImageUrlParser.extractStorageKeys(request.getContent());


        outboxPublisher.publish(
                OutboxEventType.POST_UPDATED,
                DataSerializer.serialize(
                        new PostUpdatedOutboxEvent(post.getId(), userId, keys)
                ),
                OutboxEventKey.postUpdated(postId)
        );

        return PostDetailResponse.from(post, getAuthorOrUnknown(userId));
    }

    @Override
    public void delete(Long boardId, Long postId, Long userId) {
        Post post = postRepository.findActiveById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        if (!post.getBoard().getId().equals(boardId)) {
            throw new CustomException(ErrorCode.INVALID_BOARD_POST_RELATION);
        }

        if (!post.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        post.delete(LocalDateTime.now(clock));

        outboxPublisher.publish(
                OutboxEventType.POST_DELETED,
                DataSerializer.serialize(
                        new PostDeletedOutboxEvent(post.getId(), post.getUserId())
                ),
                OutboxEventKey.postDeleted(postId)
        );
    }

    @Override
    public boolean like(Long boardId, Long postId, Long userId) {
        Post post = postRepository.findActiveById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        if (!post.getBoard().getId().equals(boardId)) {
            throw new CustomException(ErrorCode.INVALID_BOARD_POST_RELATION);
        }

        return postRedisService.toggleLike(postId, userId);
    }

    private AuthorSummary getAuthorOrUnknown(Long userId) {
        try {
            return userClient.findAuthorsByIds(List.of(userId))
                    .getOrDefault(userId, AuthorSummary.unknown());
        } catch (RuntimeException e) {
            log.warn("[POST_COMMAND][AUTHOR_LOOKUP_FAIL] userId={}", userId, e);
            return AuthorSummary.unknown();
        }
    }
}
