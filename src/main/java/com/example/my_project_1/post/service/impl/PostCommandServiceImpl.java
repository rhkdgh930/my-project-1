package com.example.my_project_1.post.service.impl;

import com.example.my_project_1.board.domain.Board;
import com.example.my_project_1.board.repository.BoardRepository;
import com.example.my_project_1.common.exception.CustomException;
import com.example.my_project_1.common.exception.ErrorCode;
import com.example.my_project_1.post.domain.Post;
import com.example.my_project_1.post.event.PostCreatedEvent;
import com.example.my_project_1.post.event.PostUpdatedEvent;
import com.example.my_project_1.post.repository.PostRepository;
import com.example.my_project_1.post.service.PostCommandService;
import com.example.my_project_1.post.service.PostRedisService;
import com.example.my_project_1.post.service.request.PostCreateRequest;
import com.example.my_project_1.post.service.request.PostUpdateRequest;
import com.example.my_project_1.post.service.response.PostDetailResponse;
import com.example.my_project_1.image.domain.ImageOwnerType;
import com.example.my_project_1.image.service.ImageService;
import com.example.my_project_1.image.utils.ImageUrlParser;
import com.example.my_project_1.user.client.UserClient;
import com.example.my_project_1.user.client.UserSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Transactional
@RequiredArgsConstructor
@Service
public class PostCommandServiceImpl implements PostCommandService {
    private final BoardRepository boardRepository;
    private final PostRepository postRepository;
    private final PostRedisService postRedisService;
    private final UserClient userClient;
    private final ApplicationEventPublisher eventPublisher;

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

        eventPublisher.publishEvent(
                new PostCreatedEvent(post.getId(), userId, keys));

        String nickname = getNickname(userId);

        return PostDetailResponse.from(post, nickname);
    }

    @Override
    public PostDetailResponse update(Long boardId, Long postId, Long userId, PostUpdateRequest request) {

        Post post = postRepository.findByIdAndDeletedAtIsNull(postId)
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

        eventPublisher.publishEvent(
                new PostUpdatedEvent(post.getId(), userId, keys)
        );

        String nickname = getNickname(userId);

        return PostDetailResponse.from(post, nickname);
    }

    @Override
    public boolean like(Long boardId, Long postId, Long userId) {
        Post post = postRepository.findByIdAndDeletedAtIsNull(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        if (!post.getBoard().getId().equals(boardId)) {
            throw new CustomException(ErrorCode.INVALID_BOARD_POST_RELATION);
        }

        return postRedisService.toggleLike(postId, userId);
    }

    private String getNickname(Long userId) {
        Map<Long, UserSummary> userMap = userClient.findUsersByIds(List.of(userId));
        UserSummary user = userMap.get(userId);
        return (user != null) ? user.nickname() : "알 수 없는 사용자";
    }
}
