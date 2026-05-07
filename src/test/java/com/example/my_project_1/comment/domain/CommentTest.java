package com.example.my_project_1.comment.domain;

import com.example.my_project_1.common.exception.CustomException;
import com.example.my_project_1.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommentTest {

    @Test
    @DisplayName("댓글 삭제는 deletedAt을 세팅하고 tombstone content로 마스킹한다.")
    void delete_setsDeletedAtAndMasksContent() {
        Comment comment = Comment.createRoot(1L, 10L, "content");
        LocalDateTime now = LocalDateTime.of(2026, 5, 1, 0, 0);

        comment.delete(10L, now);

        assertThat(comment.isDeleted()).isTrue();
        assertThat(comment.getDeletedAt()).isEqualTo(now);
        assertThat(comment.getContent()).isEqualTo(Comment.DELETED_CONTENT);
    }

    @Test
    @DisplayName("이미 삭제된 댓글을 다시 삭제하면 idempotent하게 성공한다.")
    void delete_isIdempotentWhenAlreadyDeleted() {
        Comment comment = Comment.createRoot(1L, 10L, "content");
        LocalDateTime first = LocalDateTime.of(2026, 5, 1, 0, 0);
        LocalDateTime second = first.plusMinutes(1);

        comment.delete(10L, first);
        comment.delete(10L, second);

        assertThat(comment.getDeletedAt()).isEqualTo(first);
        assertThat(comment.getContent()).isEqualTo(Comment.DELETED_CONTENT);
    }

    @Test
    @DisplayName("삭제된 댓글은 수정할 수 없다.")
    void update_rejectsDeletedComment() {
        Comment comment = Comment.createRoot(1L, 10L, "content");
        comment.delete(10L, LocalDateTime.of(2026, 5, 1, 0, 0));

        assertThatThrownBy(() -> comment.updateContent("updated", 10L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COMMENT_ALREADY_DELETED);
    }

    @Test
    @DisplayName("댓글 수정은 null 내용을 거부한다.")
    void update_rejectsNullContent() {
        Comment comment = Comment.createRoot(1L, 10L, "content");

        assertThatThrownBy(() -> comment.updateContent(null, 10L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("댓글 수정은 blank 내용을 거부한다.")
    void update_rejectsBlankContent() {
        Comment comment = Comment.createRoot(1L, 10L, "content");

        assertThatThrownBy(() -> comment.updateContent("   ", 10L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("댓글 수정은 1000자를 초과하는 내용을 거부한다.")
    void update_rejectsContentLongerThan1000() {
        Comment comment = Comment.createRoot(1L, 10L, "content");
        String tooLong = "a".repeat(1001);

        assertThatThrownBy(() -> comment.updateContent(tooLong, 10L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("루트 댓글 생성은 null 또는 blank 내용을 거부한다.")
    void createRoot_rejectsNullOrBlankContent() {
        assertThatThrownBy(() -> Comment.createRoot(1L, 10L, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Comment.createRoot(1L, 10L, "   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("루트 댓글 생성은 1000자를 초과하는 내용을 거부한다.")
    void createRoot_rejectsContentLongerThan1000() {
        String tooLong = "a".repeat(1001);

        assertThatThrownBy(() -> Comment.createRoot(1L, 10L, tooLong))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("대댓글 생성은 null 또는 blank 내용을 거부한다.")
    void createReply_rejectsNullOrBlankContent() {
        Comment parent = Comment.createRoot(1L, 10L, "content");
        ReflectionTestUtils.setField(parent, "id", 100L);

        assertThatThrownBy(() -> Comment.createReply(parent, 20L, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Comment.createReply(parent, 20L, "   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("대댓글 생성은 1000자를 초과하는 내용을 거부한다.")
    void createReply_rejectsContentLongerThan1000() {
        Comment parent = Comment.createRoot(1L, 10L, "content");
        ReflectionTestUtils.setField(parent, "id", 100L);
        String tooLong = "a".repeat(1001);

        assertThatThrownBy(() -> Comment.createReply(parent, 20L, tooLong))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("삭제된 댓글에는 답글을 작성할 수 없다.")
    void createReply_rejectsDeletedParent() {
        Comment parent = Comment.createRoot(1L, 10L, "content");
        ReflectionTestUtils.setField(parent, "id", 100L);
        parent.delete(10L, LocalDateTime.of(2026, 5, 1, 0, 0));

        assertThatThrownBy(() -> Comment.createReply(parent, 20L, "reply"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COMMENT_ALREADY_DELETED);
    }
}
