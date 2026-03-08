package com.example.my_project_1.board.domain;

import com.example.my_project_1.board.service.request.BoardCreateRequest;
import com.example.my_project_1.common.exception.CustomException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

class BoardTest {
    private final static String NAME = "test board name";
    private final static String DESCRIPTION = "test board description";

    @DisplayName("게시판 생성에 성공합니다.")
    @Test
    void board_create_success_test() {
        Board board = getBoard();
        assertThat(board.getName()).isEqualTo(NAME);
        assertThat(board.getDescription()).isEqualTo(DESCRIPTION);
    }

    @DisplayName("게시판명이 null일 경우 게시판 생성에 실패합니다.")
    @Test
    void board_create_fail_test() {
        assertThatThrownBy(() -> Board.create(null, DESCRIPTION))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("게시판명은 필수입니다.");

    }

    @DisplayName("게시판 수정에 성공합니다.")
    @Test
    void board_update_success_test() {
        Board board = getBoard();
        assertThat(board.getName()).isEqualTo(NAME);
        assertThat(board.getDescription()).isEqualTo(DESCRIPTION);

        String newName = "new board name";
        String newDescription = "new board description";

        board.update(newName, newDescription);

        assertThat(board.getName()).isEqualTo(newName);
        assertThat(board.getDescription()).isEqualTo(newDescription);
    }

    @DisplayName("게시판명이 널값 혹은 빈칸일 경우 게시판 수정을 실패합니다.")
    @Test
    void board_update_fail_test_name_is_blank() {
        Board board = getBoard();
        assertThat(board.getName()).isEqualTo(NAME);
        assertThat(board.getDescription()).isEqualTo(DESCRIPTION);

        board_update_fail_test(board, null);
        board_update_fail_test(board, " ");
    }

    private static void board_update_fail_test(Board board, String newName) {
        String newDescription = "new board description";

        assertThatThrownBy(() -> board.update(newName, newDescription))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("게시판명은 필수입니다.");
    }

    @DisplayName("게시판 soft delete를 성공합니다.")
    @Test
    void board_soft_delete_success_test() {
        Board board = getBoard();
        assertThat(board.getName()).isEqualTo(NAME);
        assertThat(board.getDescription()).isEqualTo(DESCRIPTION);

        String uuid = "12345678";
        String maskedName = NAME + "_deleted_" + uuid;
        board.maskBoardData(uuid);

        assertThat(board.getName()).isEqualTo(maskedName);
        assertThat(board.getDescription()).isEqualTo("삭제된 게시판 입니다.");
    }

    private static Board getBoard() {
        BoardCreateRequest request = getBoardCreateRequest();
        return Board.create(request.getName(), request.getDescription());
    }

    private static BoardCreateRequest getBoardCreateRequest() {
        return BoardFixture.CreateBoardRequest();
    }
}