package com.example.my_project_1.comment.service;

import com.example.my_project_1.comment.domain.Comment;
import com.example.my_project_1.comment.repository.CommentRepository;
import com.example.my_project_1.comment.service.impl.CommentCommandServiceImpl;
import com.example.my_project_1.comment.service.response.CommentResponse;
import com.example.my_project_1.common.exception.CustomException;
import com.example.my_project_1.common.exception.ErrorCode;
import com.example.my_project_1.post.domain.Post;
import com.example.my_project_1.post.repository.PostRepository;
import com.example.my_project_1.user.client.AuthorStatus;
import com.example.my_project_1.user.client.AuthorSummary;
import com.example.my_project_1.user.client.UserClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommentCommandServiceImplTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-05-01T00:00:00Z"),
            ZoneId.of("UTC")
    );

    private CommentRepository commentRepository;
    private PostRepository postRepository;
    private UserClient userClient;
    private CommentCommandServiceImpl commentCommandService;

    @BeforeEach
    void setUp() {
        commentRepository = mock(CommentRepository.class);
        postRepository = mock(PostRepository.class);
        userClient = mock(UserClient.class);
        commentCommandService = new CommentCommandServiceImpl(CLOCK, commentRepository, postRepository, userClient);
    }

    @Test
    @DisplayName("루트 댓글 작성은 active post만 허용한다.")
    void writeComment_requiresActivePost() {
        Long postId = 1L;
        when(postRepository.findActiveById(postId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentCommandService.writeComment(postId, 10L, "content"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.POST_NOT_FOUND);

        verify(commentRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("대댓글 작성은 부모 댓글의 post가 active post인지 검증한다.")
    void writeReply_requiresParentActivePost() {
        Comment parent = comment(1L, 100L, 10L, "parent");
        when(commentRepository.findById(100L)).thenReturn(Optional.of(parent));
        when(postRepository.findActiveById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentCommandService.writeReply(1L, 100L, 20L, "reply"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.POST_NOT_FOUND);

        verify(commentRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("삭제된 부모 댓글에는 대댓글을 작성할 수 없다.")
    void writeReply_rejectsDeletedParent() {
        Comment parent = comment(1L, 100L, 10L, "parent");
        parent.delete(10L, LocalDateTime.now(CLOCK));
        when(commentRepository.findById(100L)).thenReturn(Optional.of(parent));
        when(postRepository.findActiveById(1L)).thenReturn(Optional.of(mock(Post.class)));

        assertThatThrownBy(() -> commentCommandService.writeReply(1L, 100L, 20L, "reply"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COMMENT_ALREADY_DELETED);

        verify(commentRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("다른 게시글의 댓글에는 대댓글을 작성할 수 없다.")
    void writeReply_rejectsWhenUrlPostIdDoesNotMatchParentPostId() {
        Comment parent = comment(2L, 100L, 10L, "parent");
        when(commentRepository.findById(100L)).thenReturn(Optional.of(parent));

        assertThatThrownBy(() -> commentCommandService.writeReply(1L, 100L, 20L, "reply"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_COMMENT_POST_RELATION);

        verify(postRepository, never()).findActiveById(org.mockito.ArgumentMatchers.anyLong());
        verify(commentRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("댓글 삭제는 tombstone 상태로 만든다.")
    void delete_setsTombstone() {
        Comment comment = comment(1L, 100L, 10L, "content");
        when(commentRepository.findById(100L)).thenReturn(Optional.of(comment));
        when(postRepository.findActiveById(1L)).thenReturn(Optional.of(mock(Post.class)));

        commentCommandService.delete(1L, 100L, 10L);

        assertThat(comment.isDeleted()).isTrue();
        assertThat(comment.getDeletedAt()).isEqualTo(LocalDateTime.now(CLOCK));
        assertThat(comment.getContent()).isEqualTo(Comment.DELETED_CONTENT);
    }

    @Test
    @DisplayName("댓글 수정은 URL postId와 active post를 검증하고 CommentResponse를 반환한다.")
    void update_validatesPostAndReturnsCommentResponse() {
        Comment comment = comment(1L, 100L, 10L, "content");
        when(commentRepository.findById(100L)).thenReturn(Optional.of(comment));
        when(postRepository.findActiveById(1L)).thenReturn(Optional.of(mock(Post.class)));
        when(userClient.findAuthorsByIds(List.of(10L)))
                .thenReturn(Map.of(10L, AuthorSummary.active(10L, "author")));

        CommentResponse response = commentCommandService.update(1L, 100L, 10L, "updated");

        assertThat(comment.getContent()).isEqualTo("updated");
        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getContent()).isEqualTo("updated");
        assertThat(response.getAuthorId()).isEqualTo(10L);
        assertThat(response.getAuthor().displayName()).isEqualTo("author");
        assertThat(response.getAuthor().status()).isEqualTo(AuthorStatus.ACTIVE);
        verify(postRepository).findActiveById(1L);
    }

    @Test
    @DisplayName("댓글 수정 응답은 작성자 조회 실패 시 UNKNOWN author로 fallback한다.")
    void update_usesUnknownAuthorWhenAuthorLookupThrows() {
        Comment comment = comment(1L, 100L, 10L, "content");
        when(commentRepository.findById(100L)).thenReturn(Optional.of(comment));
        when(postRepository.findActiveById(1L)).thenReturn(Optional.of(mock(Post.class)));
        when(userClient.findAuthorsByIds(List.of(10L)))
                .thenThrow(new RuntimeException("author lookup failed"));

        CommentResponse response = commentCommandService.update(1L, 100L, 10L, "updated");

        assertThat(response.getAuthor().id()).isNull();
        assertThat(response.getAuthor().status()).isEqualTo(AuthorStatus.UNKNOWN);
    }

    @Test
    @DisplayName("댓글 수정은 comment가 없으면 COMMENT_NOT_FOUND로 거절한다.")
    void update_rejectsWhenCommentNotFound() {
        when(commentRepository.findById(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentCommandService.update(1L, 100L, 10L, "updated"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COMMENT_NOT_FOUND);

        verify(postRepository, never()).findActiveById(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    @DisplayName("댓글 수정은 URL postId와 comment postId가 다르면 거절한다.")
    void update_rejectsWhenUrlPostIdDoesNotMatchCommentPostId() {
        Comment comment = comment(2L, 100L, 10L, "content");
        when(commentRepository.findById(100L)).thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentCommandService.update(1L, 100L, 10L, "updated"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_COMMENT_POST_RELATION);

        verify(postRepository, never()).findActiveById(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    @DisplayName("댓글 수정은 active post가 아니면 POST_NOT_FOUND로 거절한다.")
    void update_rejectsWhenPostIsNotActive() {
        Comment comment = comment(1L, 100L, 10L, "content");
        when(commentRepository.findById(100L)).thenReturn(Optional.of(comment));
        when(postRepository.findActiveById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentCommandService.update(1L, 100L, 10L, "updated"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.POST_NOT_FOUND);
    }

    @Test
    @DisplayName("댓글 수정은 작성자가 아니면 ACCESS_DENIED로 거절한다.")
    void update_rejectsWhenUserIsNotAuthor() {
        Comment comment = comment(1L, 100L, 10L, "content");
        when(commentRepository.findById(100L)).thenReturn(Optional.of(comment));
        when(postRepository.findActiveById(1L)).thenReturn(Optional.of(mock(Post.class)));

        assertThatThrownBy(() -> commentCommandService.update(1L, 100L, 20L, "updated"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("댓글 수정은 삭제된 comment면 COMMENT_ALREADY_DELETED로 거절한다.")
    void update_rejectsDeletedComment() {
        Comment comment = comment(1L, 100L, 10L, "content");
        comment.delete(10L, LocalDateTime.now(CLOCK));
        when(commentRepository.findById(100L)).thenReturn(Optional.of(comment));
        when(postRepository.findActiveById(1L)).thenReturn(Optional.of(mock(Post.class)));

        assertThatThrownBy(() -> commentCommandService.update(1L, 100L, 10L, "updated"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COMMENT_ALREADY_DELETED);
    }

    @Test
    @DisplayName("이미 삭제된 댓글을 같은 작성자가 다시 삭제하면 idempotent하게 성공한다.")
    void delete_isIdempotentForSameAuthor() {
        Comment comment = comment(1L, 100L, 10L, "content");
        LocalDateTime firstDeletedAt = LocalDateTime.of(2026, 4, 30, 0, 0);
        comment.delete(10L, firstDeletedAt);
        when(commentRepository.findById(100L)).thenReturn(Optional.of(comment));
        when(postRepository.findActiveById(1L)).thenReturn(Optional.of(mock(Post.class)));

        commentCommandService.delete(1L, 100L, 10L);

        assertThat(comment.getDeletedAt()).isEqualTo(firstDeletedAt);
        assertThat(comment.getContent()).isEqualTo(Comment.DELETED_CONTENT);
    }

    @Test
    @DisplayName("댓글 삭제는 comment가 없으면 COMMENT_NOT_FOUND로 거절한다.")
    void delete_rejectsWhenCommentNotFound() {
        when(commentRepository.findById(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentCommandService.delete(1L, 100L, 10L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COMMENT_NOT_FOUND);

        verify(postRepository, never()).findActiveById(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    @DisplayName("댓글 삭제는 URL postId와 comment postId가 다르면 거절한다.")
    void delete_rejectsWhenUrlPostIdDoesNotMatchCommentPostId() {
        Comment comment = comment(2L, 100L, 10L, "content");
        when(commentRepository.findById(100L)).thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentCommandService.delete(1L, 100L, 10L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_COMMENT_POST_RELATION);

        verify(postRepository, never()).findActiveById(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    @DisplayName("댓글 삭제는 active post가 아니면 POST_NOT_FOUND로 거절한다.")
    void delete_rejectsWhenPostIsNotActive() {
        Comment comment = comment(1L, 100L, 10L, "content");
        when(commentRepository.findById(100L)).thenReturn(Optional.of(comment));
        when(postRepository.findActiveById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentCommandService.delete(1L, 100L, 10L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.POST_NOT_FOUND);
    }

    @Test
    @DisplayName("댓글 삭제는 작성자가 아니면 ACCESS_DENIED로 거절한다.")
    void delete_rejectsWhenUserIsNotAuthor() {
        Comment comment = comment(1L, 100L, 10L, "content");
        when(commentRepository.findById(100L)).thenReturn(Optional.of(comment));
        when(postRepository.findActiveById(1L)).thenReturn(Optional.of(mock(Post.class)));

        assertThatThrownBy(() -> commentCommandService.delete(1L, 100L, 20L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ACCESS_DENIED);
    }

    private static Comment comment(Long postId, Long commentId, Long userId, String content) {
        Comment comment = Comment.createRoot(postId, userId, content);
        ReflectionTestUtils.setField(comment, "id", commentId);
        return comment;
    }
}
