package com.example.my_project_1.post.event;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostDeletedOutboxEvent {

    private Long postId;
    private Long userId;

    public PostDeletedOutboxEvent(Long postId, Long userId) {
        this.postId = postId;
        this.userId = userId;
    }
}
