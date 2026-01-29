package com.example.my_project_1.auth.utils;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;

public class CookieUtils {
    public static void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .path("/")
                .secure(false)       // HTTPS에서만 전송 (로컬 개발시 false로 풀어야 할 수도 있음. 일단 true 권장), true or false
                .sameSite("Lax")   // 크로스 도메인 요청 허용 (프론트/백엔드 도메인 다를 때 필수) None or Lax
                .httpOnly(true)     // 자바스크립트 접근 불가 (XSS 방지)
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
