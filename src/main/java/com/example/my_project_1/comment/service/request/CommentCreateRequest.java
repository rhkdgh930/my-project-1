package com.example.my_project_1.comment.service.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class CommentCreateRequest {
    @NotBlank
    @Size(max = 1000)
    private String content;
}
