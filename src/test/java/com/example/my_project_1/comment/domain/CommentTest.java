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
