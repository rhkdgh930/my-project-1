package com.example.my_project_1.user.client;

public record AuthorSummary(
        Long id,
        String displayName,
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
