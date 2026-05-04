package com.example.my_project_1.comment.service;

import com.example.my_project_1.comment.domain.Comment;
import com.example.my_project_1.comment.repository.CommentRepository;
import com.example.my_project_1.common.exception.CustomException;
import com.example.my_project_1.common.exception.ErrorCode;
import com.example.my_project_1.post.domain.Post;
import com.example.my_project_1.post.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
    private CommentCommandServiceImpl commentCommandService;

    @BeforeEach
    void setUp() {
        commentRepository = mock(CommentRepository.class);
        postRepository = mock(PostRepository.class);
        commentCommandService = new CommentCommandServiceImpl(CLOCK, commentRepository, postRepository);
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

        commentCommandService.delete(100L, 10L);

        assertThat(comment.isDeleted()).isTrue();
        assertThat(comment.getDeletedAt()).isEqualTo(LocalDateTime.now(CLOCK));
        assertThat(comment.getContent()).isEqualTo(Comment.DELETED_CONTENT);
    }

    private static Comment comment(Long postId, Long commentId, Long userId, String content) {
        Comment comment = Comment.createRoot(postId, userId, content);
        ReflectionTestUtils.setField(comment, "id", commentId);
        return comment;
    }
}
