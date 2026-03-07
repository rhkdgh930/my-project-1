package com.example.my_project_1.auth.utils;

public class UrlUtils {
    public static final String[] PERMITTED = {

            //swagger
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-ui.html",
            "/api-docs/**",

            //auth
            "/api/auth/login",
            "/api/auth/reissue",
            "/api/auth/logout",
            "/api/auth/restore",

            //OAuth
            "/api/auth/test",
            "/login/oauth2/code/google",
            "oauth2/authorization/google",

            //user
            "/api/users/signup",
            "/api/users/emails/verification/**",
            "/api/users/password-reset/**",

            //board
            "/api/boards/**",

            //post
            "/api/boards/*/posts",

            //comment
            "/api/posts/*/comments/**",

            //image
            "/favicon.ico",
            "/images/**",

    };
}
