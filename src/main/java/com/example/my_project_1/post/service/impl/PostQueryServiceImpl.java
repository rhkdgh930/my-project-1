package com.example.my_project_1.post.service.impl;

import com.example.my_project_1.board.repository.BoardRepository;
import com.example.my_project_1.common.exception.CustomException;
import com.example.my_project_1.common.exception.ErrorCode;
import com.example.my_project_1.common.utils.PageResponse;
import com.example.my_project_1.post.domain.Post;
import com.example.my_project_1.post.repository.PostRepository;
import com.example.my_project_1.post.service.PostQueryService;
import com.example.my_project_1.post.service.PostRedisService;
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
    private final UserClient userClient;
    private final PostRedisService postRedisService;

    @Override
    public PageResponse<PostListResponse> getPosts(Long boardId, Pageable pageable) {
        validateBoard(boardId);
        // 1. DB에서 해당 게시판의 게시글만 페이징 조회 (Deleted=False)
        Page<Post> page = postRepository.findAllActiveByBoardId(boardId, pageable);

        if (page.isEmpty()) {
            return PageResponse.of(Page.empty(pageable));
        }

        // 2. 작성자 정보 일괄 조회 (N+1 방지)
        // 페이지 내의 모든 작성자 ID를 중복 없이 추출
        List<Long> authorIds = page.getContent().stream()
                .map(Post::getUserId)
                .distinct()
                .toList();

        // UserClient를 통해 유저 정보 Map으로 가져오기 (authorId -> UserSummary)
        Map<Long, AuthorSummary> authorMap = findAuthorsForList(boardId, authorIds);

        // 3. DTO 변환 (작성자 닉네임 포함)
        Page<PostListResponse> dtoPage = page.map(post -> {
            AuthorSummary author = authorMap.getOrDefault(post.getUserId(), AuthorSummary.unknown());

            // PostListResponse.from에 nickname을 전달하도록 수정하거나
            // 직접 빌더/생성자로 매핑
            PostListResponse response = PostListResponse.from(post, author);
            response.updateCounts(
                    countOrDefault(postRedisService.getViewOrNull(post.getId()), post.getViewCount()),
                    countOrDefault(postRedisService.getLikeOrNull(post.getId()), post.getLikeCount())
            );
            return response;
        });

        // 4. 공통 페이징 포맷으로 반환
        return PageResponse.of(dtoPage);
    }

    private void validateBoard(Long boardId) {
        boardRepository.findByIdAndDeletedAtIsNull(boardId)
                .orElseThrow(() -> new CustomException(ErrorCode.BOARD_NOT_FOUND));
    }

    @Override
    public PostDetailResponse getPostDetail(Long boardId, Long postId) {
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
                countOrDefault(postRedisService.getLikeOrNull(postId), post.getLikeCount())
        );
        return response;
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
