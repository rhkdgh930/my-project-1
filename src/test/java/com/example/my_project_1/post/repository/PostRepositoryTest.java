package com.example.my_project_1.post.repository;

import com.example.my_project_1.board.domain.Board;
import com.example.my_project_1.comment.domain.Comment;
import com.example.my_project_1.common.config.QueryDslConfig;
import com.example.my_project_1.post.domain.Post;
import com.example.my_project_1.post.domain.PostLike;
import com.example.my_project_1.post.domain.PostTag;
import com.example.my_project_1.post.domain.Tag;
import com.example.my_project_1.post.service.request.PostSearchCondition;
import com.example.my_project_1.post.service.request.PostSearchType;
import com.example.my_project_1.post.service.request.PostSortType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.datasource.url=jdbc:h2:mem:post-repository-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver"
})
@Import(QueryDslConfig.class)
class PostRepositoryTest {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostLikeRepository postLikeRepository;

    @Autowired
    private PostTagRepository postTagRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("searchActivePosts는 기본 최신순을 사용하고 inactive post를 제외한다.")
    void searchActivePosts_usesDefaultLatestAndExcludesInactivePosts() {
        Post first = postRepository.save(post(board("board-a"), 100L, "first", "content"));
        Board board = first.getBoard();
        Post second = postRepository.save(post(board, 101L, "second", "content"));
        postRepository.save(post(board("board-b"), 102L, "other board", "content"));
        Post deletedPost = postRepository.save(post(board, 103L, "deleted", "content"));
        deletedPost.delete(LocalDateTime.of(2026, 5, 12, 10, 0));
        Board deletedBoard = board("deleted-board");
        deletedBoard.delete(LocalDateTime.of(2026, 5, 12, 10, 0));
        postRepository.save(post(deletedBoard, 104L, "deleted board post", "content"));
        postRepository.flush();

        Page<Post> result = postRepository.searchActivePosts(
                board.getId(),
                null,
                PageRequest.of(0, 10)
        );

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
                .extracting(Post::getId)
                .containsExactly(second.getId(), first.getId());
    }

    @Test
    @DisplayName("searchActivePosts는 제목, 내용, 제목+내용 키워드 검색을 적용한다.")
    void searchActivePosts_appliesKeywordSearch() {
        Board board = board("search-board");
        Post titleMatch = postRepository.save(post(board, 100L, "redis title", "plain body"));
        Post contentMatch = postRepository.save(post(board, 101L, "plain title", "redis body"));
        postRepository.save(post(board, 102L, "plain title", "plain body"));
        postRepository.flush();

        assertThat(search(board, "redis", PostSearchType.TITLE, PostSortType.OLDEST).getContent())
                .extracting(Post::getId)
                .containsExactly(titleMatch.getId());
        assertThat(search(board, "redis", PostSearchType.CONTENT, PostSortType.OLDEST).getContent())
                .extracting(Post::getId)
                .containsExactly(contentMatch.getId());
        assertThat(search(board, "redis", PostSearchType.TITLE_CONTENT, PostSortType.OLDEST).getContent())
                .extracting(Post::getId)
                .containsExactly(titleMatch.getId(), contentMatch.getId());
    }

    @Test
    @DisplayName("빈 키워드와 null 조건은 검색 필터 없이 기본 정렬을 사용한다.")
    void searchActivePosts_usesDefaultsForBlankKeywordAndNullTypes() {
        Board board = board("default-board");
        Post first = postRepository.save(post(board, 100L, "first", "content"));
        Post second = postRepository.save(post(board, 101L, "second", "content"));
        PostSearchCondition condition = new PostSearchCondition();
        condition.setKeyword("   ");
        condition.setSearchType(null);
        condition.setSortType(null);

        Page<Post> result = postRepository.searchActivePosts(
                board.getId(),
                condition,
                PageRequest.of(0, 10)
        );

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
                .extracting(Post::getId)
                .containsExactly(second.getId(), first.getId());
    }

    @Test
    @DisplayName("searchActivePosts는 오래된순, 조회수순, 좋아요순 정렬과 페이징을 지원한다.")
    void searchActivePosts_supportsSortingAndPaging() {
        Board board = board("sort-board");
        Post low = postRepository.save(post(board, 100L, "low", "content"));
        low.updateCounts(1L, 10L);
        Post highView = postRepository.save(post(board, 101L, "high view", "content"));
        highView.updateCounts(30L, 1L);
        Post highLike = postRepository.save(post(board, 102L, "high like", "content"));
        highLike.updateCounts(5L, 40L);
        postRepository.flush();

        assertThat(sorted(board, PostSortType.OLDEST, 0, 10).getContent())
                .extracting(Post::getId)
                .containsExactly(low.getId(), highView.getId(), highLike.getId());
        assertThat(sorted(board, PostSortType.VIEW_COUNT, 0, 10).getContent())
                .extracting(Post::getId)
                .containsExactly(highView.getId(), highLike.getId(), low.getId());
        assertThat(sorted(board, PostSortType.LIKE_COUNT, 0, 10).getContent())
                .extracting(Post::getId)
                .containsExactly(highLike.getId(), low.getId(), highView.getId());
        assertThat(sorted(board, PostSortType.OLDEST, 1, 1).getContent())
                .extracting(Post::getId)
                .containsExactly(highView.getId());
    }

    @Test
    @DisplayName("post_like는 postId와 userId 조합을 unique로 보장한다.")
    void postLike_enforcesUniquePostAndUser() {
        Post post = postRepository.save(post(board("like-board"), 100L, "title", "content"));
        postLikeRepository.saveAndFlush(PostLike.create(post.getId(), 200L));

        assertThatThrownBy(() -> postLikeRepository.saveAndFlush(PostLike.create(post.getId(), 200L)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("post_like는 다른 사용자의 좋아요 row를 별도로 유지한다.")
    void postLike_keepsOtherUsersLikes() {
        Post post = postRepository.save(post(board("multi-like-board"), 100L, "title", "content"));
        postLikeRepository.saveAndFlush(PostLike.create(post.getId(), 200L));
        PostLike second = postLikeRepository.saveAndFlush(PostLike.create(post.getId(), 201L));

        int deletedCount = postLikeRepository.deleteByPostIdAndUserId(post.getId(), 200L);
        postLikeRepository.flush();

        assertThat(deletedCount).isEqualTo(1);
        assertThat(postLikeRepository.findById(second.getId())).isPresent();
        assertThat(postLikeRepository.deleteByPostIdAndUserId(post.getId(), 200L)).isZero();
        assertThat(postLikeRepository.existsByPostIdAndUserId(post.getId(), 200L)).isFalse();
        assertThat(postLikeRepository.existsByPostIdAndUserId(post.getId(), 201L)).isTrue();
    }

    @Test
    @DisplayName("findLikedActivePostsByUserId는 현재 사용자가 좋아요한 활성 게시글을 좋아요 최신순으로 조회한다.")
    void findLikedActivePostsByUserId_returnsLikedActivePostsInLikedLatestOrder() {
        Long userId = 200L;
        Board board = board("liked-board");
        Post oldLiked = postRepository.save(post(board, 100L, "old liked", "content"));
        Post latestLiked = postRepository.save(post(board, 101L, "latest liked", "content"));
        Post otherUserLiked = postRepository.save(post(board, 102L, "other user liked", "content"));
        Post deletedPost = postRepository.save(post(board, 103L, "deleted", "content"));
        deletedPost.delete(LocalDateTime.of(2026, 5, 12, 10, 0));
        Board deletedBoard = board("deleted-liked-board");
        deletedBoard.delete(LocalDateTime.of(2026, 5, 12, 10, 0));
        Post deletedBoardPost = postRepository.save(post(deletedBoard, 104L, "deleted board post", "content"));

        postLikeRepository.save(like(oldLiked.getId(), userId, LocalDateTime.of(2026, 5, 13, 10, 0)));
        postLikeRepository.save(like(latestLiked.getId(), userId, LocalDateTime.of(2026, 5, 14, 10, 0)));
        postLikeRepository.save(like(otherUserLiked.getId(), 201L, LocalDateTime.of(2026, 5, 15, 10, 0)));
        postLikeRepository.save(like(deletedPost.getId(), userId, LocalDateTime.of(2026, 5, 16, 10, 0)));
        postLikeRepository.save(like(deletedBoardPost.getId(), userId, LocalDateTime.of(2026, 5, 17, 10, 0)));
        postLikeRepository.flush();
        entityManager.clear();

        Page<Post> result = postRepository.findLikedActivePostsByUserId(userId, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
                .extracting(Post::getId)
                .containsExactly(latestLiked.getId(), oldLiked.getId());
    }

    @Test
    @DisplayName("findLikedActivePostsByUserId는 좋아요한 게시글이 없으면 빈 페이지를 반환한다.")
    void findLikedActivePostsByUserId_returnsEmptyPageWhenUserHasNoLikes() {
        Page<Post> result = postRepository.findLikedActivePostsByUserId(999L, PageRequest.of(0, 10));

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("findActivePostsByUserId는 현재 사용자가 작성한 활성 게시글을 최신순으로 조회한다.")
    void findActivePostsByUserId_returnsMyActivePostsInLatestOrder() {
        Long userId = 200L;
        Board board = board("my-post-board");
        Post oldPost = postRepository.save(post(board, userId, "old", "content"));
        Post latestPost = postRepository.save(post(board, userId, "latest", "content"));
        postRepository.save(post(board, 201L, "other user", "content"));
        Post deletedPost = postRepository.save(post(board, userId, "deleted", "content"));
        deletedPost.delete(LocalDateTime.of(2026, 5, 12, 10, 0));
        Board deletedBoard = board("deleted-my-post-board");
        deletedBoard.delete(LocalDateTime.of(2026, 5, 12, 10, 0));
        postRepository.save(post(deletedBoard, userId, "deleted board post", "content"));
        postRepository.flush();

        Page<Post> result = postRepository.findActivePostsByUserId(userId, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
                .extracting(Post::getId)
                .containsExactly(latestPost.getId(), oldPost.getId());
    }

    @Test
    @DisplayName("findActivePostsByUserId는 작성한 게시글이 없으면 빈 페이지를 반환한다.")
    void findActivePostsByUserId_returnsEmptyPageWhenUserHasNoPosts() {
        Page<Post> result = postRepository.findActivePostsByUserId(999L, PageRequest.of(0, 10));

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("findCommentedActivePostsByUserId는 내가 댓글 단 활성 게시글을 댓글 최신순으로 중복 없이 조회한다.")
    void findCommentedActivePostsByUserId_returnsDistinctPostsInLatestCommentOrder() {
        Long userId = 200L;
        Board board = board("commented-board");
        Post duplicatePost = postRepository.save(post(board, 100L, "duplicate", "content"));
        Post latestPost = postRepository.save(post(board, 101L, "latest", "content"));
        Post replyPost = postRepository.save(post(board, 102L, "reply", "content"));
        Post otherUserPost = postRepository.save(post(board, 103L, "other user", "content"));
        Post deletedCommentOnlyPost = postRepository.save(post(board, 104L, "deleted comment only", "content"));
        Post deletedPost = postRepository.save(post(board, 105L, "deleted post", "content"));
        deletedPost.delete(LocalDateTime.of(2026, 5, 12, 10, 0));
        Board deletedBoard = board("deleted-commented-board");
        deletedBoard.delete(LocalDateTime.of(2026, 5, 12, 10, 0));
        Post deletedBoardPost = postRepository.save(post(deletedBoard, 106L, "deleted board post", "content"));
        postRepository.flush();

        rootComment(duplicatePost, userId, LocalDateTime.of(2026, 5, 13, 10, 0));
        rootComment(duplicatePost, userId, LocalDateTime.of(2026, 5, 15, 10, 0));
        rootComment(latestPost, userId, LocalDateTime.of(2026, 5, 16, 10, 0));
        rootComment(otherUserPost, 201L, LocalDateTime.of(2026, 5, 19, 10, 0));
        Comment deletedComment = rootComment(deletedCommentOnlyPost, userId, LocalDateTime.of(2026, 5, 20, 10, 0));
        deletedComment.delete(userId, LocalDateTime.of(2026, 5, 20, 11, 0));
        rootComment(deletedPost, userId, LocalDateTime.of(2026, 5, 21, 10, 0));
        rootComment(deletedBoardPost, userId, LocalDateTime.of(2026, 5, 22, 10, 0));
        Comment parent = rootComment(replyPost, 201L, LocalDateTime.of(2026, 5, 17, 10, 0));
        replyComment(parent, userId, LocalDateTime.of(2026, 5, 18, 10, 0));
        entityManager.flush();
        entityManager.clear();

        Page<Post> result = postRepository.findCommentedActivePostsByUserId(userId, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getContent())
                .extracting(Post::getId)
                .containsExactly(replyPost.getId(), latestPost.getId(), duplicatePost.getId());
    }

    @Test
    @DisplayName("findCommentedActivePostsByUserId는 내가 댓글 단 게시글이 없으면 빈 페이지를 반환한다.")
    void findCommentedActivePostsByUserId_returnsEmptyPageWhenUserHasNoComments() {
        Page<Post> result = postRepository.findCommentedActivePostsByUserId(999L, PageRequest.of(0, 10));

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("태그명으로 활성 게시글을 최신순으로 조회하고 삭제된 게시글과 다른 태그 게시글은 제외한다.")
    void findActivePostsByTagName_returnsActivePostsInLatestOrder() {
        Board board = board("tag-board");
        Tag spring = tagRepository.saveAndFlush(Tag.create("Spring"));
        Tag redis = tagRepository.saveAndFlush(Tag.create("Redis"));
        Post oldTagged = postRepository.save(post(board, 100L, "old tagged", "content"));
        Post latestTagged = postRepository.save(post(board, 101L, "latest tagged", "content"));
        Post otherTagged = postRepository.save(post(board, 102L, "other tagged", "content"));
        Post deletedPost = postRepository.save(post(board, 103L, "deleted", "content"));
        deletedPost.delete(LocalDateTime.of(2026, 5, 12, 10, 0));
        Board deletedBoard = board("deleted-tag-board");
        deletedBoard.delete(LocalDateTime.of(2026, 5, 12, 10, 0));
        Post deletedBoardPost = postRepository.save(post(deletedBoard, 104L, "deleted board post", "content"));
        postRepository.flush();

        postTagRepository.save(PostTag.create(oldTagged.getId(), spring.getId()));
        postTagRepository.save(PostTag.create(latestTagged.getId(), spring.getId()));
        postTagRepository.save(PostTag.create(otherTagged.getId(), redis.getId()));
        postTagRepository.save(PostTag.create(deletedPost.getId(), spring.getId()));
        postTagRepository.save(PostTag.create(deletedBoardPost.getId(), spring.getId()));
        postTagRepository.flush();
        entityManager.clear();

        Page<Post> result = postRepository.findActivePostsByTagName("Spring", PageRequest.of(0, 10));
        Page<Post> missing = postRepository.findActivePostsByTagName("Missing", PageRequest.of(0, 10));
        Page<Post> lowerCase = postRepository.findActivePostsByTagName("spring", PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
                .extracting(Post::getId)
                .containsExactly(latestTagged.getId(), oldTagged.getId());
        assertThat(missing.getContent()).isEmpty();
        assertThat(lowerCase.getContent()).isEmpty();
    }

    @Test
    @DisplayName("findPopularActivePosts는 likeCount * 3 + viewCount 점수순과 id 역순으로 활성 게시글을 조회한다.")
    void findPopularActivePosts_returnsActivePostsInScoreOrder() {
        Board board = board("popular-board");
        Post low = postRepository.save(post(board, 100L, "low", "content"));
        low.updateCounts(10L, 1L); // 13
        Post sameScoreOld = postRepository.save(post(board, 101L, "same old", "content"));
        sameScoreOld.updateCounts(12L, 1L); // 15
        Post sameScoreNew = postRepository.save(post(board, 102L, "same new", "content"));
        sameScoreNew.updateCounts(9L, 2L); // 15
        Post high = postRepository.save(post(board, 103L, "high", "content"));
        high.updateCounts(20L, 2L); // 26

        postRepository.save(post(board("other-popular-board"), 104L, "other board", "content"));
        Post deletedPost = postRepository.save(post(board, 105L, "deleted", "content"));
        deletedPost.updateCounts(100L, 100L);
        deletedPost.delete(LocalDateTime.of(2026, 5, 12, 10, 0));
        Board deletedBoard = board("deleted-popular-board");
        deletedBoard.delete(LocalDateTime.of(2026, 5, 12, 10, 0));
        Post deletedBoardPost = postRepository.save(post(deletedBoard, 106L, "deleted board post", "content"));
        deletedBoardPost.updateCounts(100L, 100L);
        postRepository.flush();
        entityManager.clear();

        List<Post> result = postRepository.findPopularActivePosts(board.getId(), 3);

        assertThat(result)
                .extracting(Post::getId)
                .containsExactly(high.getId(), sameScoreNew.getId(), sameScoreOld.getId());
    }

    @Test
    @DisplayName("updateLikeCountDelta는 likeCount를 원자적으로 증감하고 음수로 만들지 않는다.")
    void updateLikeCountDelta_updatesAtomicallyAndDoesNotGoNegative() {
        Post post = postRepository.save(post(board("delta-board"), 100L, "title", "content"));
        post.updateCounts(0L, 0L);
        postRepository.flush();
        entityManager.clear();

        postRepository.updateLikeCountDelta(post.getId(), -1L);
        entityManager.clear();

        assertThat(postRepository.findById(post.getId()).orElseThrow().getLikeCount()).isZero();

        postRepository.updateLikeCountDelta(post.getId(), 1L);
        postRepository.updateLikeCountDelta(post.getId(), 1L);
        postRepository.updateLikeCountDelta(post.getId(), -1L);
        entityManager.clear();

        assertThat(postRepository.findById(post.getId()).orElseThrow().getLikeCount()).isEqualTo(1L);
    }

    private Page<Post> search(
            Board board,
            String keyword,
            PostSearchType searchType,
            PostSortType sortType
    ) {
        PostSearchCondition condition = new PostSearchCondition();
        condition.setKeyword(keyword);
        condition.setSearchType(searchType);
        condition.setSortType(sortType);
        return postRepository.searchActivePosts(board.getId(), condition, PageRequest.of(0, 10));
    }

    private Page<Post> sorted(Board board, PostSortType sortType, int page, int size) {
        PostSearchCondition condition = new PostSearchCondition();
        condition.setSortType(sortType);
        return postRepository.searchActivePosts(board.getId(), condition, PageRequest.of(page, size));
    }

    private Board board(String name) {
        Board board = Board.create(name, "description");
        entityManager.persist(board);
        return board;
    }

    private static Post post(Board board, Long userId, String title, String content) {
        return Post.create(board, userId, title, content);
    }

    private static PostLike like(Long postId, Long userId, LocalDateTime createdAt) {
        PostLike postLike = PostLike.create(postId, userId);
        ReflectionTestUtils.setField(postLike, "createdAt", createdAt);
        return postLike;
    }

    private Comment rootComment(Post post, Long userId, LocalDateTime createdAt) {
        Comment comment = Comment.createRoot(post.getId(), userId, "content");
        entityManager.persist(comment);
        ReflectionTestUtils.setField(comment, "createdAt", createdAt);
        return comment;
    }

    private Comment replyComment(Comment parent, Long userId, LocalDateTime createdAt) {
        Comment comment = Comment.createReply(parent, userId, "reply");
        entityManager.persist(comment);
        ReflectionTestUtils.setField(comment, "createdAt", createdAt);
        return comment;
    }
}
