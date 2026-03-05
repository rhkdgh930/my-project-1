package com.example.my_project_1.auth.utils;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;

public class CookieUtils {
    public static void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .path("/")
                .secure(false)
                .sameSite("Lax")
                .httpOnly(true)
                .maxAge(maxAge)
                .build();

        response.addHeader("Set-Cookie", cookie.toString());
    }

    public static void deleteCookie(HttpServletResponse response, String name) {
        ResponseCookie cookie = ResponseCookie.from(name, "")
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }
}
