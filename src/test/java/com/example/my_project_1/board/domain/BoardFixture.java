package com.example.my_project_1.board.domain;

import com.example.my_project_1.board.service.request.BoardCreateRequest;

public class BoardFixture {
    private final static String NAME = "test board name";
    private final static String DESCRIPTION = "test board description";

    static BoardCreateRequest CreateBoardRequest() {
        return BoardCreateRequest.create(NAME, DESCRIPTION);
    }
}
