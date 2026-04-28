package com.example.my_project_1.auth.handler;

import com.example.my_project_1.auth.exception.UserSuspendedException;
import com.example.my_project_1.auth.service.RedisLoginAttemptService;
import com.example.my_project_1.common.exception.ErrorCode;
import com.example.my_project_1.user.domain.SuspensionReason;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class JwtLoginFailureHandlerTest {

    private final RedisLoginAttemptService loginAttemptService = mock(RedisLoginAttemptService.class);
    private final JwtLoginFailureHandler handler = new JwtLoginFailureHandler(loginAttemptService);

    @Test
    @DisplayName("영구 정지 계정은 suspendedUntil이 없어도 NPE 없이 USER_SUSPENDED 응답을 반환한다.")
    void permanentSuspensionReturnsUserSuspendedResponseWithoutNpe() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        UserSuspendedException exception = new UserSuspendedException(
                "suspended",
                null,
                SuspensionReason.OTHER
        );

        handler.onAuthenticationFailure(request, response, exception);

        assertThat(response.getStatus()).isEqualTo(ErrorCode.USER_SUSPENDED.getHttpStatus().value());
        assertThat(response.getContentAsString()).contains("\"code\":\"USER_SUSPENDED\"");
        assertThat(response.getContentAsString()).contains("\"permanent\":true");
        assertThat(response.getContentAsString()).contains("\"suspendedUntil\":null");
    }
}
