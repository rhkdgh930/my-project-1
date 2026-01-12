package com.example.my_project_1.post.service.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class PostCreateRequest {
    @NotBlank
    private String title;
    @NotBlank
    private String content;
}
