package com.example.my_project_1.post.service.impl;

import com.example.my_project_1.board.repository.BoardRepository;
import com.example.my_project_1.common.exception.CustomException;
import com.example.my_project_1.common.exception.ErrorCode;
import com.example.my_project_1.common.utils.PageResponse;
import com.example.my_project_1.post.domain.Post;
import com.example.my_project_1.post.repository.PostLikeRepository;
import com.example.my_project_1.post.repository.PostRepository;
import com.example.my_project_1.post.service.PostQueryService;
import com.example.my_project_1.post.service.PostRedisService;
import com.example.my_project_1.post.service.request.PostSearchCondition;
import com.example.my_project_1.post.service.response.PostListResponse;
import com.example.my_project_1.post.service.response.PostDetailResponse;
import com.example.my_project_1.user.client.AuthorSummary;
import com.example.my_project_1.user.client.UserClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
@Service
public class PostQueryServiceImpl implements PostQueryService {

    private final BoardRepository boardRepository;
    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final UserClient userClient;
    private final PostRedisService postRedisService;

    @Override
    public PageResponse<PostListResponse> getPosts(
            Long boardId,
            PostSearchCondition condition,
            Pageable pageable
    ) {
        validateBoard(boardId);

        Page<Post> page = postRepository.searchActivePosts(
                boardId,
                condition,
                pageable
        );

        List<Long> authorIds = page.getContent().stream()
                .map(Post::getUserId)
                .distinct()
                .toList();

        Map<Long, AuthorSummary> authorMap = authorIds.isEmpty()
                ? Map.of()
                : findAuthorsForList(boardId, authorIds);

        Page<PostListResponse> dtoPage = page.map(post -> {
            AuthorSummary author = authorMap.getOrDefault(
                    post.getUserId(),
                    AuthorSummary.unknown()
            );

            PostListResponse response = PostListResponse.from(post, author);
            response.updateCounts(
                    countOrDefault(postRedisService.getViewOrNull(post.getId()), post.getViewCount()),
                    post.getLikeCount()
            );
            return response;
        });

        return PageResponse.of(dtoPage);
    }

    private void validateBoard(Long boardId) {
        boardRepository.findByIdAndDeletedAtIsNull(boardId)
                .orElseThrow(() -> new CustomException(ErrorCode.BOARD_NOT_FOUND));
    }

    @Override
    public PostDetailResponse getPostDetail(Long boardId, Long postId) {
        return getPostDetail(boardId, postId, null);
    }

    @Override
    public PostDetailResponse getPostDetail(Long boardId, Long postId, Long currentUserId) {
        Post post = postRepository.findActiveById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        if (!post.getBoard().getId().equals(boardId)) {
            throw new CustomException(ErrorCode.INVALID_BOARD_POST_RELATION);
        }

        postRedisService.increaseView(postId);

        Map<Long, AuthorSummary> authorMap = findAuthorsForDetail(boardId, postId, List.of(post.getUserId()));
        AuthorSummary author = authorMap.getOrDefault(post.getUserId(), AuthorSummary.unknown());

        PostDetailResponse response = PostDetailResponse.from(post, author);
        response.updateCounts(
                countOrDefault(postRedisService.getViewOrNull(postId), post.getViewCount()),
                post.getLikeCount()
        );
        response.updateLikedByMe(isLikedByMe(postId, currentUserId));
        return response;
    }

    private boolean isLikedByMe(Long postId, Long currentUserId) {
        return currentUserId != null && postLikeRepository.existsByPostIdAndUserId(postId, currentUserId);
    }

    private long countOrDefault(Long redisCount, long dbCount) {
        return redisCount != null ? redisCount : dbCount;
    }

    private Map<Long, AuthorSummary> findAuthorsForList(Long boardId, List<Long> authorIds) {
        try {
            return userClient.findAuthorsByIds(authorIds);
        } catch (RuntimeException e) {
            log.warn("[POST_QUERY][AUTHOR_LOOKUP_FAIL] boardId={} authorIds={}", boardId, authorIds, e);
            return Map.of();
        }
    }

    private Map<Long, AuthorSummary> findAuthorsForDetail(Long boardId, Long postId, List<Long> authorIds) {
        try {
            return userClient.findAuthorsByIds(authorIds);
        } catch (RuntimeException e) {
            log.warn("[POST_QUERY][AUTHOR_LOOKUP_FAIL] boardId={} postId={} authorIds={}", boardId, postId, authorIds, e);
            return Map.of();
        }
    }
}
