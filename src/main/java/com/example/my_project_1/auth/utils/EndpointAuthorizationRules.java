package com.example.my_project_1.auth.utils;

public final class EndpointAuthorizationRules {

    private EndpointAuthorizationRules() {
    }

    /**
     * HTTP method와 관계없이 public이어도 되는 경로.
     *
     * 주의:
     * - /api/admin/** 경로를 넣지 않는다.
     * - board/post/comment 같은 REST resource wildcard를 넣지 않는다.
     * - write API가 포함될 수 있는 wildcard를 넣지 않는다.
     */
    public static final String[] PUBLIC_ANY_METHOD = {
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-ui.html",
            "/api-docs/**",

            "/api/auth/login",
            "/api/auth/reissue",
            "/api/auth/logout",
            "/api/auth/restore",

            "/api/auth/test",
            "/login/oauth2/code/google",
            "/oauth2/authorization/google",

            "/api/users/signup",
            "/api/users/emails/verification/**",
            "/api/users/password-reset/**",

            "/favicon.ico",
            "/images/**"
    };

    /**
     * GET method만 public인 조회 API.
     * POST/PATCH/DELETE는 authenticated로 내려가야 한다.
     */
    public static final String[] PUBLIC_GET_ONLY = {
            "/api/boards",
            "/api/boards/*",
            "/api/boards/*/posts",
            "/api/boards/*/posts/*",
            "/api/posts/*/comments"
    };

    /**
     * 관리자 전용 API.
     */
    public static final String[] ADMIN_ONLY = {
            "/api/admin/**"
    };

    /**
     * Public user endpoints를 제외한 user API는 인증이 필요하다.
     */
    public static final String[] USER_ONLY = {
            "/api/users/**"
    };
}