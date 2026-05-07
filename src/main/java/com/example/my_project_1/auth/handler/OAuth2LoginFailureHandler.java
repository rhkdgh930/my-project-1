package com.example.my_project_1.auth.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${app.oauth2.failure-redirect-path:/login}")
    private String failureRedirectPath;

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException {
        String targetUrl = UriComponentsBuilder.fromUriString(frontendUrl)
                .path(failureRedirectPath)
                .queryParam("oauth2Error", "true")
                .build()
                .toUriString();

        response.sendRedirect(targetUrl);
    }
}