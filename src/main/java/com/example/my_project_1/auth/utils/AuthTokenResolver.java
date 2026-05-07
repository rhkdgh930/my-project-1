package com.example.my_project_1.auth.utils;

import com.example.my_project_1.auth.constant.SecurityConstants;
import com.example.my_project_1.common.exception.CustomException;
import com.example.my_project_1.common.exception.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AuthTokenResolver {

    public String resolveRequiredRefreshToken(
            String refreshTokenCookie,
            String refreshTokenHeader
    ) {
        String refreshToken = resolveOptionalRefreshToken(refreshTokenCookie, refreshTokenHeader);

        if (!StringUtils.hasText(refreshToken)) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        return refreshToken;
    }

    public String resolveOptionalBearerToken(String authorizationHeader) {
        if (!StringUtils.hasText(authorizationHeader)) {
            return null;
        }

        if (!authorizationHeader.startsWith(SecurityConstants.BEARER)) {
            throw new CustomException(ErrorCode.INVALID_ACCESS_TOKEN);
        }

        String token = authorizationHeader.substring(SecurityConstants.BEARER.length());

        if (!StringUtils.hasText(token)) {
            throw new CustomException(ErrorCode.INVALID_ACCESS_TOKEN);
        }

        return token;
    }

    public String resolveOptionalRefreshToken(
            String refreshTokenCookie,
            String refreshTokenHeader
    ) {
        boolean hasCookie = StringUtils.hasText(refreshTokenCookie);
        boolean hasHeader = StringUtils.hasText(refreshTokenHeader);

        if (hasCookie && hasHeader) {
            if (!refreshTokenCookie.equals(refreshTokenHeader)) {
                throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
            }

            return refreshTokenCookie;
        }

        if (hasCookie) {
            return refreshTokenCookie;
        }

        if (hasHeader) {
            return refreshTokenHeader;
        }

        return null;
    }
}