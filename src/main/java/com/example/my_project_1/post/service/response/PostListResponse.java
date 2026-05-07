package com.example.my_project_1.post.service.response;

import com.example.my_project_1.post.domain.Post;
import com.example.my_project_1.user.client.AuthorSummary;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Schema(description = "게시글 목록 응답 항목")
public class PostListResponse {
    @Schema(description = "게시글 ID", example = "1")
    private Long postId;

    @Schema(description = "게시글 제목", example = "첫 번째 게시글")
    private String title;

    @Schema(description = "전환 기간 유지 필드입니다. author.displayName과 같은 값입니다.", example = "seoul_dev")
    private String nickname;

    @Schema(description = "작성자 표시 정보. ACTIVE/WITHDRAWN/SUSPENDED/UNKNOWN 상태를 포함합니다.")
    private AuthorSummary author;

    @Schema(description = "응답 시점 기준 조회 수", example = "10")
    private long viewCount;

    @Schema(description = "응답 시점 기준 좋아요 수", example = "3")
    private long likeCount;

    @Schema(description = "작성 시각", example = "2026-05-06T10:15:30")
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
