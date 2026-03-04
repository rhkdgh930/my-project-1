package com.example.my_project_1.post.service.response;

import com.example.my_project_1.post.domain.Post;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class PostListResponse {
    private Long postId;
    private String title;
    private String nickname;
    private long viewCount;
    private long likeCount;
    private LocalDateTime createdAt;

    public static PostListResponse from(Post post, String nickname) {
        PostListResponse response = new PostListResponse();
        response.postId = post.getId();
        response.title = post.getTitle();
        response.nickname = nickname;
        response.viewCount = post.getViewCount();
        response.likeCount = post.getLikeCount();
        response.createdAt = post.getCreatedAt();
        return response;
    }

    public void updateCounts(long viewCount, long likeCount) {
        this.viewCount = viewCount;
        this.likeCount = likeCount;
    }
}
