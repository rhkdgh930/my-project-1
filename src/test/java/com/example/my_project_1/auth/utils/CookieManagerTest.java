package com.example.my_project_1.auth.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class CookieManagerTest {

    @Test
    @DisplayName("refresh token cookie는 CookieProperties 설정을 반영해 생성된다.")
    void addRefreshTokenCookie_usesCookieProperties() {
        CookieProperties properties = cookieProperties();
        properties.setSecure(true);
        properties.setSameSite("None");
        properties.setDomain("example.com");
        CookieManager cookieManager = new CookieManager(properties);
        MockHttpServletResponse response = new MockHttpServletResponse();

        cookieManager.addRefreshTokenCookie(response, "refresh-token", 60);

        assertThat(response.getHeader("Set-Cookie"))
                .contains("refreshToken=refresh-token")
                .contains("Path=/auth")
                .contains("Domain=example.com")
                .contains("Max-Age=60")
                .contains("Secure")
                .contains("HttpOnly")
                .contains("SameSite=None");
    }

    @Test
    @DisplayName("refresh token 삭제 cookie는 maxAge 0과 동일한 속성을 사용한다.")
    void deleteRefreshTokenCookie_usesCookieProperties() {
        CookieProperties properties = cookieProperties();
        CookieManager cookieManager = new CookieManager(properties);
        MockHttpServletResponse response = new MockHttpServletResponse();

        cookieManager.deleteRefreshTokenCookie(response);

        assertThat(response.getHeader("Set-Cookie"))
                .contains("refreshToken=")
                .contains("Path=/auth")
                .contains("Max-Age=0")
                .contains("HttpOnly")
                .contains("SameSite=Lax");
    }

    private CookieProperties cookieProperties() {
        CookieProperties properties = new CookieProperties();
        properties.setRefreshTokenName("refreshToken");
        properties.setPath("/auth");
        properties.setHttpOnly(true);
        properties.setSecure(false);
        properties.setSameSite("Lax");
        return properties;
    }
}
