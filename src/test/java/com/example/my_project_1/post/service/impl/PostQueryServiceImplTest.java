package com.example.my_project_1.post.service.impl;

import com.example.my_project_1.board.domain.Board;
import com.example.my_project_1.board.repository.BoardRepository;
import com.example.my_project_1.common.exception.CustomException;
import com.example.my_project_1.common.exception.ErrorCode;
import com.example.my_project_1.common.utils.PageResponse;
import com.example.my_project_1.post.domain.Post;
import com.example.my_project_1.post.repository.PostLikeRepository;
import com.example.my_project_1.post.repository.PostRepository;
import com.example.my_project_1.post.service.PostRedisService;
import com.example.my_project_1.post.service.request.PostSearchCondition;
import com.example.my_project_1.post.service.request.PostSortType;
import com.example.my_project_1.post.service.response.PostDetailResponse;
import com.example.my_project_1.post.service.response.PostListResponse;
import com.example.my_project_1.user.client.AuthorStatus;
import com.example.my_project_1.user.client.AuthorSummary;
import com.example.my_project_1.user.client.UserClient;
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
import static org.mockito.ArgumentMatchers.any;

class PostQueryServiceImplTest {

    private static final String PROFILE_IMAGE_URL = "/images/550e8400-e29b-41d4-a716-446655440000.png";

    private BoardRepository boardRepository;
    private PostRepository postRepository;
    private PostLikeRepository postLikeRepository;
    private UserClient userClient;
    private PostRedisService postRedisService;
    private PostQueryServiceImpl postQueryService;

    @BeforeEach
    void setUp() {
        boardRepository = mock(BoardRepository.class);
        postRepository = mock(PostRepository.class);
        postLikeRepository = mock(PostLikeRepository.class);
        userClient = mock(UserClient.class);
        postRedisService = mock(PostRedisService.class);
        postQueryService = new PostQueryServiceImpl(boardRepository, postRepository, postLikeRepository, userClient, postRedisService);
    }

    @Test
    @DisplayName("게시글 목록 조회는 게시글과 게시판이 모두 삭제되지 않은 active post만 조회한다.")
    void getPosts_usesActiveBoardPostQuery() {
        Long boardId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        Post post = post(boardId, 10L, 100L);

        when(boardRepository.findByIdAndDeletedAtIsNull(boardId))
                .thenReturn(Optional.ofNullable(Board.create("board", "description")));
        when(postRepository.searchActivePosts(boardId, null, pageable))
                .thenReturn(new PageImpl<>(List.of(post), pageable, 1));
        when(userClient.findAuthorsByIds(List.of(100L)))
                .thenReturn(Map.of(100L, AuthorSummary.active(100L, "nickname", PROFILE_IMAGE_URL)));
        when(postRedisService.getViewOrNull(10L)).thenReturn(3L);

        PageResponse<PostListResponse> response = postQueryService.getPosts(boardId, null, pageable);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getPostId()).isEqualTo(10L);
        assertThat(response.getContent().get(0).getNickname()).isEqualTo("nickname");
        assertThat(response.getContent().get(0).getAuthor().id()).isEqualTo(100L);
        assertThat(response.getContent().get(0).getAuthor().displayName()).isEqualTo("nickname");
        assertThat(response.getContent().get(0).getAuthor().status()).isEqualTo(AuthorStatus.ACTIVE);
        assertThat(response.getContent().get(0).getAuthor().profileImageUrl()).isEqualTo(PROFILE_IMAGE_URL);
        assertThat(response.getContent().get(0).getViewCount()).isEqualTo(3L);
        assertThat(response.getContent().get(0).getLikeCount()).isEqualTo(0L);
        verify(postRepository).searchActivePosts(boardId, null, pageable);
    }

    @Test
    @DisplayName("post list는 redis count가 없으면 DB count를 유지한다.")
    void getPosts_keepsDbCountsWhenRedisCountsAreMissing() {
        Long boardId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        Post post = post(boardId, 10L, 100L);
        post.updateCounts(100L, 5L);

        when(boardRepository.findByIdAndDeletedAtIsNull(boardId))
                .thenReturn(Optional.ofNullable(Board.create("board", "description")));
        when(postRepository.searchActivePosts(boardId, null, pageable))
                .thenReturn(new PageImpl<>(List.of(post), pageable, 1));
        when(userClient.findAuthorsByIds(List.of(100L)))
                .thenReturn(Map.of(100L, AuthorSummary.active(100L, "nickname")));
        when(postRedisService.getViewOrNull(10L)).thenReturn(null);

        PageResponse<PostListResponse> response = postQueryService.getPosts(boardId, null, pageable);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getViewCount()).isEqualTo(100L);
        assertThat(response.getContent().get(0).getLikeCount()).isEqualTo(5L);
    }

    @Test
    @DisplayName("post list가 빈 페이지여도 metadata를 유지하고 author 조회를 생략한다.")
    void getPosts_keepsPageMetadataAndSkipsAuthorLookupWhenPageIsEmpty() {
        Long boardId = 1L;
        Pageable pageable = PageRequest.of(2, 10);

        when(boardRepository.findByIdAndDeletedAtIsNull(boardId))
                .thenReturn(Optional.ofNullable(Board.create("board", "description")));
        when(postRepository.searchActivePosts(boardId, null, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 25));

        PageResponse<PostListResponse> response = postQueryService.getPosts(boardId, null, pageable);

        assertThat(response.getContent()).isEmpty();
        assertThat(response.getPageNumber()).isEqualTo(2);
        assertThat(response.getPageSize()).isEqualTo(10);
        assertThat(response.getTotalElements()).isEqualTo(25);
        assertThat(response.getTotalPages()).isEqualTo(3);
        verify(userClient, never()).findAuthorsByIds(any());
        verify(postRedisService, never()).getViewOrNull(any());
    }

    @Test
    @DisplayName("post list passes search condition to custom repository")
    void getPosts_passesSearchConditionToCustomRepository() {
        Long boardId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        PostSearchCondition condition = new PostSearchCondition();
        condition.setKeyword("redis");
        condition.setSortType(PostSortType.LIKE_COUNT);

        when(boardRepository.findByIdAndDeletedAtIsNull(boardId))
                .thenReturn(Optional.ofNullable(Board.create("board", "description")));
        when(postRepository.searchActivePosts(boardId, condition, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        PageResponse<PostListResponse> response = postQueryService.getPosts(boardId, condition, pageable);

        assertThat(response.getContent()).isEmpty();
        verify(postRepository).searchActivePosts(boardId, condition, pageable);
        verify(userClient, never()).findAuthorsByIds(any());
    }

    @Test
    @DisplayName("게시글 상세 조회는 active post 확인 후 조회수를 증가시킨다.")
    void getPostDetail_increasesViewAfterActivePostFound() {
        Long boardId = 1L;
        Long postId = 10L;
        Post post = post(boardId, postId, 100L);

        when(postRepository.findActiveById(postId)).thenReturn(Optional.of(post));
        when(userClient.findAuthorsByIds(List.of(100L)))
                .thenReturn(Map.of(100L, AuthorSummary.active(100L, "nickname", PROFILE_IMAGE_URL)));
        when(postRedisService.getViewOrNull(postId)).thenReturn(4L);

        PostDetailResponse response = postQueryService.getPostDetail(boardId, postId);

        assertThat(response.getPostId()).isEqualTo(postId);
        assertThat(response.getViewCount()).isEqualTo(4L);
        assertThat(response.getLikeCount()).isEqualTo(0L);
        assertThat(response.isLikedByMe()).isFalse();
        assertThat(response.getNickname()).isEqualTo("nickname");
        assertThat(response.getUserId()).isEqualTo(100L);
        assertThat(response.getAuthor().id()).isEqualTo(100L);
        assertThat(response.getAuthor().displayName()).isEqualTo("nickname");
        assertThat(response.getAuthor().status()).isEqualTo(AuthorStatus.ACTIVE);
        assertThat(response.getAuthor().profileImageUrl()).isEqualTo(PROFILE_IMAGE_URL);
        InOrder inOrder = inOrder(postRepository, postRedisService);
        inOrder.verify(postRepository).findActiveById(postId);
        inOrder.verify(postRedisService).increaseView(postId);
    }

    @Test
    @DisplayName("post detail은 redis view와 DB like를 사용한다.")
    void getPostDetail_usesRedisViewAndDbLikeWhenRedisLikeIsMissing() {
        Long boardId = 1L;
        Long postId = 10L;
        Post post = post(boardId, postId, 100L);
        post.updateCounts(100L, 5L);

        when(postRepository.findActiveById(postId)).thenReturn(Optional.of(post));
        when(userClient.findAuthorsByIds(List.of(100L)))
                .thenReturn(Map.of(100L, AuthorSummary.active(100L, "nickname")));
        when(postRedisService.getViewOrNull(postId)).thenReturn(101L);

        PostDetailResponse response = postQueryService.getPostDetail(boardId, postId);

        assertThat(response.getViewCount()).isEqualTo(101L);
        assertThat(response.getLikeCount()).isEqualTo(5L);
    }

    @Test
    @DisplayName("비로그인 상세 조회는 likedByMe=false를 반환하고 좋아요 여부를 조회하지 않는다.")
    void getPostDetail_returnsFalseLikedByMeForAnonymousUser() {
        Long boardId = 1L;
        Long postId = 10L;
        Post post = post(boardId, postId, 100L);

        when(postRepository.findActiveById(postId)).thenReturn(Optional.of(post));
        when(userClient.findAuthorsByIds(List.of(100L)))
                .thenReturn(Map.of(100L, AuthorSummary.active(100L, "nickname")));

        PostDetailResponse response = postQueryService.getPostDetail(boardId, postId, null);

        assertThat(response.isLikedByMe()).isFalse();
        verify(postLikeRepository, never()).existsByPostIdAndUserId(any(), any());
        verify(postRedisService).increaseView(postId);
    }

    @Test
    @DisplayName("로그인 사용자가 좋아요한 게시글 상세 조회는 likedByMe=true를 반환한다.")
    void getPostDetail_returnsTrueLikedByMeWhenUserLikedPost() {
        Long boardId = 1L;
        Long postId = 10L;
        Long currentUserId = 200L;
        Post post = post(boardId, postId, 100L);

        when(postRepository.findActiveById(postId)).thenReturn(Optional.of(post));
        when(userClient.findAuthorsByIds(List.of(100L)))
                .thenReturn(Map.of(100L, AuthorSummary.active(100L, "nickname")));
        when(postLikeRepository.existsByPostIdAndUserId(postId, currentUserId)).thenReturn(true);

        PostDetailResponse response = postQueryService.getPostDetail(boardId, postId, currentUserId);

        assertThat(response.isLikedByMe()).isTrue();
        verify(postLikeRepository).existsByPostIdAndUserId(postId, currentUserId);
        verify(postRedisService).increaseView(postId);
    }

    @Test
    @DisplayName("로그인 사용자가 좋아요하지 않은 게시글 상세 조회는 likedByMe=false를 반환한다.")
    void getPostDetail_returnsFalseLikedByMeWhenUserDidNotLikePost() {
        Long boardId = 1L;
        Long postId = 10L;
        Long currentUserId = 200L;
        Post post = post(boardId, postId, 100L);

        when(postRepository.findActiveById(postId)).thenReturn(Optional.of(post));
        when(userClient.findAuthorsByIds(List.of(100L)))
                .thenReturn(Map.of(100L, AuthorSummary.active(100L, "nickname")));
        when(postLikeRepository.existsByPostIdAndUserId(postId, currentUserId)).thenReturn(false);

        PostDetailResponse response = postQueryService.getPostDetail(boardId, postId, currentUserId);

        assertThat(response.isLikedByMe()).isFalse();
        verify(postLikeRepository).existsByPostIdAndUserId(postId, currentUserId);
        verify(postRedisService).increaseView(postId);
    }

    @Test
    @DisplayName("게시글 목록 조회는 작성자 조회 실패 시 UNKNOWN author fallback을 사용한다.")
    void getPosts_usesUnknownAuthorWhenUserLookupFails() {
        Long boardId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        Post post = post(boardId, 10L, 100L);

        when(boardRepository.findByIdAndDeletedAtIsNull(boardId))
                .thenReturn(Optional.ofNullable(Board.create("board", "description")));
        when(postRepository.searchActivePosts(boardId, null, pageable))
                .thenReturn(new PageImpl<>(List.of(post), pageable, 1));
        when(userClient.findAuthorsByIds(List.of(100L))).thenReturn(Map.of());

        PageResponse<PostListResponse> response = postQueryService.getPosts(boardId, null, pageable);

        PostListResponse postResponse = response.getContent().get(0);
        assertThat(postResponse.getNickname()).isEqualTo("알 수 없는 사용자");
        assertThat(postResponse.getAuthor().id()).isNull();
        assertThat(postResponse.getAuthor().displayName()).isEqualTo("알 수 없는 사용자");
        assertThat(postResponse.getAuthor().status()).isEqualTo(AuthorStatus.UNKNOWN);
    }

    @Test
    @DisplayName("post list uses UNKNOWN author when author bulk lookup throws")
    void getPosts_usesUnknownAuthorWhenUserLookupThrows() {
        Long boardId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        Post post = post(boardId, 10L, 100L);

        when(boardRepository.findByIdAndDeletedAtIsNull(boardId))
                .thenReturn(Optional.ofNullable(Board.create("board", "description")));
        when(postRepository.searchActivePosts(boardId, null, pageable))
                .thenReturn(new PageImpl<>(List.of(post), pageable, 1));
        when(userClient.findAuthorsByIds(List.of(100L)))
                .thenThrow(new RuntimeException("user lookup failed"));

        PageResponse<PostListResponse> response = postQueryService.getPosts(boardId, null, pageable);

        PostListResponse postResponse = response.getContent().get(0);
        assertThat(postResponse.getPostId()).isEqualTo(10L);
        assertThat(postResponse.getAuthor().id()).isNull();
        assertThat(postResponse.getAuthor().status()).isEqualTo(AuthorStatus.UNKNOWN);
    }

    @Test
    @DisplayName("post detail uses UNKNOWN author when author lookup throws")
    void getPostDetail_usesUnknownAuthorWhenUserLookupThrows() {
        Long boardId = 1L;
        Long postId = 10L;
        Post post = post(boardId, postId, 100L);

        when(postRepository.findActiveById(postId)).thenReturn(Optional.of(post));
        when(userClient.findAuthorsByIds(List.of(100L)))
                .thenThrow(new RuntimeException("user lookup failed"));

        PostDetailResponse response = postQueryService.getPostDetail(boardId, postId);

        assertThat(response.getPostId()).isEqualTo(postId);
        assertThat(response.getUserId()).isEqualTo(100L);
        assertThat(response.getAuthor().id()).isNull();
        assertThat(response.getAuthor().status()).isEqualTo(AuthorStatus.UNKNOWN);
    }

    @Test
    @DisplayName("게시글 상세 조회는 탈퇴 작성자 fallback author를 사용한다.")
    void getPostDetail_usesWithdrawnAuthorFallback() {
        Long boardId = 1L;
        Long postId = 10L;
        Post post = post(boardId, postId, 100L);

        when(postRepository.findActiveById(postId)).thenReturn(Optional.of(post));
        when(userClient.findAuthorsByIds(List.of(100L)))
                .thenReturn(Map.of(100L, AuthorSummary.withdrawn()));

        PostDetailResponse response = postQueryService.getPostDetail(boardId, postId);

        assertThat(response.getUserId()).isEqualTo(100L);
        assertThat(response.getNickname()).isEqualTo("탈퇴한 사용자");
        assertThat(response.getAuthor().id()).isNull();
        assertThat(response.getAuthor().displayName()).isEqualTo("탈퇴한 사용자");
        assertThat(response.getAuthor().status()).isEqualTo(AuthorStatus.WITHDRAWN);
    }

    @Test
    @DisplayName("게시글 상세 조회는 차단 작성자의 nickname을 유지하고 SUSPENDED author를 사용한다.")
    void getPostDetail_usesSuspendedAuthor() {
        Long boardId = 1L;
        Long postId = 10L;
        Post post = post(boardId, postId, 100L);

        when(postRepository.findActiveById(postId)).thenReturn(Optional.of(post));
        when(userClient.findAuthorsByIds(List.of(100L)))
                .thenReturn(Map.of(100L, AuthorSummary.suspended(100L, "nickname")));

        PostDetailResponse response = postQueryService.getPostDetail(boardId, postId);

        assertThat(response.getNickname()).isEqualTo("차단된 사용자");
        assertThat(response.getAuthor().id()).isEqualTo(100L);
        assertThat(response.getAuthor().displayName()).isEqualTo("차단된 사용자");
        assertThat(response.getAuthor().status()).isEqualTo(AuthorStatus.SUSPENDED);
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
        verify(postLikeRepository, never()).existsByPostIdAndUserId(any(), any());
    }

    private static Post post(Long boardId, Long postId, Long userId) {
        Board board = Board.create("board", "description");
        ReflectionTestUtils.setField(board, "id", boardId);
        Post post = Post.create(board, userId, "title", "content");
        ReflectionTestUtils.setField(post, "id", postId);
        return post;
    }
}
