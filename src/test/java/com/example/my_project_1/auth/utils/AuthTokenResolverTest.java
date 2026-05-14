package com.example.my_project_1.auth.utils;

import com.example.my_project_1.common.exception.CustomException;
import com.example.my_project_1.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthTokenResolverTest {

    private final AuthTokenResolver resolver = new AuthTokenResolver();

    @Test
    @DisplayName("Authorization header가 없으면 optional access token은 null을 반환한다.")
    void resolveOptionalBearerToken_returnsNullWhenMissing() {
        assertThat(resolver.resolveOptionalBearerToken(null)).isNull();
    }

    @Test
    @DisplayName("Bearer 형식이면 access token 값만 반환한다.")
    void resolveOptionalBearerToken_returnsToken() {
        assertThat(resolver.resolveOptionalBearerToken("Bearer access-token"))
                .isEqualTo("access-token");
    }

    @Test
    @DisplayName("Bearer 형식이 아니면 INVALID_ACCESS_TOKEN으로 실패한다.")
    void resolveOptionalBearerToken_rejectsInvalidFormat() {
        assertThatThrownBy(() -> resolver.resolveOptionalBearerToken("access-token"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_ACCESS_TOKEN);
    }

    @Test
    @DisplayName("refresh token cookie와 header 값이 다르면 INVALID_REFRESH_TOKEN으로 실패한다.")
    void resolveOptionalRefreshToken_rejectsMismatch() {
        assertThatThrownBy(() -> resolver.resolveOptionalRefreshToken("cookie-token", "header-token"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
    }

    @Test
    @DisplayName("required refresh token이 없으면 INVALID_REFRESH_TOKEN으로 실패한다.")
    void resolveRequiredRefreshToken_rejectsMissingToken() {
        assertThatThrownBy(() -> resolver.resolveRequiredRefreshToken(null, null))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
    }
}
