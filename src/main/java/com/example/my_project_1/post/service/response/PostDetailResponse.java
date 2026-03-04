package com.example.my_project_1.post.service.response;

import com.example.my_project_1.post.domain.Post;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class PostDetailResponse {
    private Long postId;
    private Long boardId;

    private Long userId;
    private String nickname;

    private String title;
    private String content;

    private long viewCount;
    private long likeCount;

    private LocalDateTime createdAt;

    public static PostDetailResponse from(Post post, String nickname) {
        PostDetailResponse response = new PostDetailResponse();
        response.postId = post.getId();
        response.boardId = post.getBoard().getId();
        response.userId = post.getUserId();
        response.nickname = nickname;
        response.title = post.getTitle();
        response.content = post.getContent();
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
