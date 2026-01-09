package com.example.my_project_1.board.service.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class BoardUpdateRequest {
    @NotBlank
    private String name;
    private String description;
}
