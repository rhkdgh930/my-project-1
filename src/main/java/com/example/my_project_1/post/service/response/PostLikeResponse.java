package com.example.my_project_1.post.service.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "게시글 좋아요 토글 응답")
public class PostLikeResponse {

    @Schema(description = "요청 후 좋아요 상태", example = "true")
    private boolean liked;

    @Schema(description = "응답 시점 기준 좋아요 수", example = "12")
    private long likeCount;

    public static PostLikeResponse of(boolean liked, long likeCount) {
        return new PostLikeResponse(liked, likeCount);
    }
}
