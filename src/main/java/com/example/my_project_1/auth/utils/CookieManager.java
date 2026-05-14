package com.example.my_project_1.auth.utils;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class CookieManager {

    private final CookieProperties properties;

    public void addRefreshTokenCookie(
            HttpServletResponse response,
            String value,
            int maxAge
    ) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie
                .from(properties.getRefreshTokenName(), value)
                .path(properties.getPath())
                .secure(properties.isSecure())
                .sameSite(properties.getSameSite())
                .httpOnly(properties.isHttpOnly())
                .maxAge(maxAge);

        if (StringUtils.hasText(properties.getDomain())) {
            builder.domain(properties.getDomain());
        }

        response.addHeader("Set-Cookie", builder.build().toString());
    }

    public void deleteRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie
                .from(properties.getRefreshTokenName(), "")
                .path(properties.getPath())
                .secure(properties.isSecure())
                .sameSite(properties.getSameSite())
                .httpOnly(properties.isHttpOnly())
                .maxAge(0);

        if (StringUtils.hasText(properties.getDomain())) {
            builder.domain(properties.getDomain());
        }

        response.addHeader("Set-Cookie", builder.build().toString());
    }
}