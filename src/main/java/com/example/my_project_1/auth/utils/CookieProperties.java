package com.example.my_project_1.auth.utils;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.cookie")
public class CookieProperties {

    private String refreshTokenName = "refreshToken";

    private boolean secure = false;

    private boolean httpOnly = true;

    private String sameSite = "Lax";

    private String path = "/";

    private String domain;
}