package com.example.my_project_1.post.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class PostUpdatedEvent {

    private final Long postId;
    private final Long userId;
    private final List<String> storageKeys;

}