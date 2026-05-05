package com.example.my_project_1.post.service.response;

import com.example.my_project_1.post.domain.Post;
import com.example.my_project_1.user.client.AuthorSummary;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class PostListResponse {
    private Long postId;
    private String title;
    private String nickname;
    private AuthorSummary author;
    private long viewCount;
    private long likeCount;
    private LocalDateTime createdAt;

    public static PostListResponse from(Post post, String nickname) {
        return from(post, AuthorSummary.active(post.getUserId(), nickname));
    }

    public static PostListResponse from(Post post, AuthorSummary author) {
        PostListResponse response = new PostListResponse();
        response.postId = post.getId();
        response.title = post.getTitle();
        response.nickname = author.displayName();
        response.author = author;
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
