package com.example.my_project_1.board.service.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class BoardCreateRequest {
    @NotBlank
    private String name;
    private String description;

    public static BoardCreateRequest create(String name, String description) {
        BoardCreateRequest request = new BoardCreateRequest();
        request.name = name;
        request.description = description;
        return request;
    }
}
