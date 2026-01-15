package com.example.my_project_1.auth.utils;

public class UrlUtils {
    public static final String[] PERMITTED = {
            //auth
            "/api/auth/login",
            "/api/auth/reissue",
            "/api/auth/logout",

            //OAuth
            "/api/auth/test",
            "/login/oauth2/code/google",
            "oauth2/authorization/google",

            //user
            "/api/user/signup",
            "/api/user/verify",

            //board
            "/api/boards/**",

            //post
            "/api/boards/*/posts",

            //comment
            "/api/posts/*/comments/**",

            //image
            "/favicon.ico",
            "/images/**"
    };
}
