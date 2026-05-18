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
import com.example.my_project_1.post.service.PostTagService;
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

    private static final int POPULAR_POST_DEFAULT_SIZE = 10;
    private static final int POPULAR_POST_MAX_SIZE = 50;

    private final BoardRepository boardRepository;
    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final UserClient userClient;
    private final PostRedisService postRedisService;
    private final PostTagService postTagService;

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

        Map<Long, List<String>> tagMap = findTagsForPosts(page.getContent());

        Page<PostListResponse> dtoPage = page.map(post -> {
            AuthorSummary author = authorMap.getOrDefault(
                    post.getUserId(),
                    AuthorSummary.unknown()
            );

            PostListResponse response = PostListResponse.from(post, author);
            response.updateCounts(
                    addDelta(post.getViewCount(), postRedisService.getViewDeltaOrNull(post.getId())),
                    post.getLikeCount()
            );
            response.updateTags(tagMap.getOrDefault(post.getId(), List.of()));
            return response;
        });

        return PageResponse.of(dtoPage);
    }

    @Override
    public PageResponse<PostListResponse> getLikedPosts(Long userId, Pageable pageable) {
        Page<Post> page = postRepository.findLikedActivePostsByUserId(userId, pageable);

        return toPostListPage(userId, page);
    }

    @Override
    public PageResponse<PostListResponse> getMyPosts(Long userId, Pageable pageable) {
        Page<Post> page = postRepository.findActivePostsByUserId(userId, pageable);

        return toPostListPage(userId, page);
    }

    @Override
    public PageResponse<PostListResponse> getCommentedPosts(Long userId, Pageable pageable) {
        Page<Post> page = postRepository.findCommentedActivePostsByUserId(userId, pageable);

        return toPostListPage(userId, page);
    }

    @Override
    public PageResponse<PostListResponse> getPostsByTagName(String tagName, Pageable pageable) {
        String normalizedTagName = tagName != null ? tagName.trim() : "";
        Page<Post> page = postRepository.findActivePostsByTagName(normalizedTagName, pageable);

        return toTaggedPostListPage(normalizedTagName, page);
    }

    @Override
    public List<PostListResponse> getPopularPosts(Long boardId, int size) {
        validateBoard(boardId);

        int limitedSize = limitPopularSize(size);
        List<Post> posts = postRepository.findPopularActivePosts(boardId, limitedSize);

        List<Long> authorIds = posts.stream()
                .map(Post::getUserId)
                .distinct()
                .toList();

        Map<Long, AuthorSummary> authorMap = authorIds.isEmpty()
                ? Map.of()
                : findAuthorsForList(boardId, authorIds);
        Map<Long, List<String>> tagMap = findTagsForPosts(posts);

        return posts.stream()
                .map(post -> {
                    AuthorSummary author = authorMap.getOrDefault(
                            post.getUserId(),
                            AuthorSummary.unknown()
                    );

                    PostListResponse response = PostListResponse.from(post, author);
                    response.updateCounts(
                            addDelta(post.getViewCount(), postRedisService.getViewDeltaOrNull(post.getId())),
                            post.getLikeCount()
                    );
                    response.updateTags(tagMap.getOrDefault(post.getId(), List.of()));
                    return response;
                })
                .toList();
    }

    private PageResponse<PostListResponse> toPostListPage(Long userId, Page<Post> page) {
        List<Long> authorIds = page.getContent().stream()
                .map(Post::getUserId)
                .distinct()
                .toList();

        Map<Long, AuthorSummary> authorMap = authorIds.isEmpty()
                ? Map.of()
                : findAuthorsForMePage(userId, authorIds);
        Map<Long, List<String>> tagMap = findTagsForPosts(page.getContent());

        Page<PostListResponse> dtoPage = page.map(post -> {
            AuthorSummary author = authorMap.getOrDefault(
                    post.getUserId(),
                    AuthorSummary.unknown()
            );

            PostListResponse response = PostListResponse.from(post, author);
            response.updateCounts(
                    addDelta(post.getViewCount(), postRedisService.getViewDeltaOrNull(post.getId())),
                    post.getLikeCount()
            );
            response.updateTags(tagMap.getOrDefault(post.getId(), List.of()));
            return response;
        });

        return PageResponse.of(dtoPage);
    }

    private PageResponse<PostListResponse> toTaggedPostListPage(String tagName, Page<Post> page) {
        List<Long> authorIds = page.getContent().stream()
                .map(Post::getUserId)
                .distinct()
                .toList();

        Map<Long, AuthorSummary> authorMap = authorIds.isEmpty()
                ? Map.of()
                : findAuthorsForTagPage(tagName, authorIds);
        Map<Long, List<String>> tagMap = findTagsForPosts(page.getContent());

        Page<PostListResponse> dtoPage = page.map(post -> {
            AuthorSummary author = authorMap.getOrDefault(
                    post.getUserId(),
                    AuthorSummary.unknown()
            );

            PostListResponse response = PostListResponse.from(post, author);
            response.updateCounts(
                    addDelta(post.getViewCount(), postRedisService.getViewDeltaOrNull(post.getId())),
                    post.getLikeCount()
            );
            response.updateTags(tagMap.getOrDefault(post.getId(), List.of()));
            return response;
        });

        return PageResponse.of(dtoPage);
    }

    private void validateBoard(Long boardId) {
        boardRepository.findByIdAndDeletedAtIsNull(boardId)
                .orElseThrow(() -> new CustomException(ErrorCode.BOARD_NOT_FOUND));
    }

    private int limitPopularSize(int size) {
        int requestedSize = size > 0 ? size : POPULAR_POST_DEFAULT_SIZE;
        return Math.min(requestedSize, POPULAR_POST_MAX_SIZE);
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
                addDelta(post.getViewCount(), postRedisService.getViewDeltaOrNull(postId)),
                post.getLikeCount()
        );
        response.updateLikedByMe(isLikedByMe(postId, currentUserId));
        response.updateTags(findTagsForPosts(List.of(post)).getOrDefault(postId, List.of()));
        return response;
    }

    private Map<Long, List<String>> findTagsForPosts(List<Post> posts) {
        List<Long> postIds = posts.stream()
                .map(Post::getId)
                .toList();
        Map<Long, List<String>> tagMap = postTagService.findTagNamesByPostIds(postIds);
        return tagMap != null ? tagMap : Map.of();
    }

    private boolean isLikedByMe(Long postId, Long currentUserId) {
        return currentUserId != null && postLikeRepository.existsByPostIdAndUserId(postId, currentUserId);
    }

    private long addDelta(long dbCount, Long redisDelta) {
        return dbCount + (redisDelta == null ? 0 : redisDelta);
    }

    private Map<Long, AuthorSummary> findAuthorsForList(Long boardId, List<Long> authorIds) {
        try {
            return userClient.findAuthorsByIds(authorIds);
        } catch (RuntimeException e) {
            log.warn("[POST_QUERY][AUTHOR_LOOKUP_FAIL] boardId={} authorIds={}", boardId, authorIds, e);
            return Map.of();
        }
    }

    private Map<Long, AuthorSummary> findAuthorsForMePage(Long userId, List<Long> authorIds) {
        try {
            return userClient.findAuthorsByIds(authorIds);
        } catch (RuntimeException e) {
            log.warn("[POST_QUERY][AUTHOR_LOOKUP_FAIL] likedUserId={} authorIds={}", userId, authorIds, e);
            return Map.of();
        }
    }

    private Map<Long, AuthorSummary> findAuthorsForTagPage(String tagName, List<Long> authorIds) {
        try {
            return userClient.findAuthorsByIds(authorIds);
        } catch (RuntimeException e) {
            log.warn("[POST_QUERY][AUTHOR_LOOKUP_FAIL] tagName={} authorIds={}", tagName, authorIds, e);
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
