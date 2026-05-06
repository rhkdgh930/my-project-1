package com.example.my_project_1.user.client;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "게시글/댓글 작성자 표시용 요약 정보. ACTIVE는 id=userId/displayName=nickname, WITHDRAWN은 id=null/displayName=탈퇴한 사용자, SUSPENDED는 id=userId/displayName=차단된 사용자, UNKNOWN은 id=null/displayName=알 수 없는 사용자입니다.")
public record AuthorSummary(
        @Schema(description = "표시 가능한 작성자 ID. 탈퇴/알 수 없음 상태에서는 null입니다.", example = "1", nullable = true)
        Long id,

        @Schema(description = "작성자 표시명", example = "seoul_dev")
        String displayName,

        @Schema(description = "작성자 표시 상태")
        AuthorStatus status
) {
    private static final String WITHDRAWN_DISPLAY_NAME = "탈퇴한 사용자";
    private static final String SUSPENDED_DISPLAY_NAME = "차단된 사용자";
    private static final String UNKNOWN_DISPLAY_NAME = "알 수 없는 사용자";

    public static AuthorSummary active(Long userId, String nickname) {
        return new AuthorSummary(userId, nickname, AuthorStatus.ACTIVE);
    }

    public static AuthorSummary withdrawn() {
        return new AuthorSummary(null, WITHDRAWN_DISPLAY_NAME, AuthorStatus.WITHDRAWN);
    }

    public static AuthorSummary suspended(Long userId, String nickname) {
        return suspended(userId);
    }

    public static AuthorSummary suspended(Long userId) {
        return new AuthorSummary(userId, SUSPENDED_DISPLAY_NAME, AuthorStatus.SUSPENDED);
    }

    public static AuthorSummary unknown() {
        return new AuthorSummary(null, UNKNOWN_DISPLAY_NAME, AuthorStatus.UNKNOWN);
    }
}
