package com.example.my_project_1.post.domain;

import com.example.my_project_1.board.domain.Board;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class PostTest {

    @Test
    @DisplayName("delete는 title/content를 마스킹하고 deletedAt을 세팅한다.")
    void delete_masksContentAndRecordsDeletedAt() {
        Board board = Board.create("board", "description");
        Post post = Post.create(board, 1L, "title", "content");
        LocalDateTime now = LocalDateTime.of(2026, 5, 6, 10, 0);

        post.delete(now);

        assertThat(post.getTitle()).isEqualTo("삭제된 게시글입니다.");
        assertThat(post.getContent()).isEqualTo("삭제된 게시글입니다.");
        assertThat(post.getDeletedAt()).isEqualTo(now);
    }
}
