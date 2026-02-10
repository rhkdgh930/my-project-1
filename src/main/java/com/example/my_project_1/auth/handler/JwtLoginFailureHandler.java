package com.example.my_project_1.auth.handler;

import com.example.my_project_1.auth.exception.LoginFailException;
import com.example.my_project_1.auth.exception.UserSuspendedException;
import com.example.my_project_1.auth.exception.WithdrawalCompletedException;
import com.example.my_project_1.auth.exception.WithdrawalPendingException;
import com.example.my_project_1.auth.service.RedisLoginAttemptService;
import com.example.my_project_1.common.exception.ErrorCode;
import com.example.my_project_1.common.exception.ExceptionResponse;
import com.example.my_project_1.common.utils.DataSerializer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtLoginFailureHandler implements AuthenticationFailureHandler {
    private final RedisLoginAttemptService loginAttemptService;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException {
        ErrorCode errorCode;
        Object data = null;

        String email = (String) request.getAttribute("email");

        if (exception instanceof UserSuspendedException suspendedEx) {
            errorCode = ErrorCode.USER_SUSPENDED;
            data = putSuspendedData(suspendedEx);
        } else if (exception instanceof WithdrawalPendingException pendingEx) {
            errorCode = ErrorCode.WITHDRAWAL_PENDING;
            data = putWithdrawalPendingData(pendingEx);
        } else {

            loginFail(email);
            if (exception instanceof LoginFailException) {
                errorCode = ErrorCode.TOO_MANY_LOGIN_FAIL;
            } else if (exception instanceof WithdrawalCompletedException) {
                errorCode = ErrorCode.WITHDRAWAL_COMPLETED;
            } else if (exception instanceof LockedException) {
                errorCode = ErrorCode.USER_SUSPENDED;
            } else if (exception instanceof DisabledException) {
                errorCode = ErrorCode.USER_NOT_FOUND;
            } else if (exception instanceof UsernameNotFoundException || exception instanceof BadCredentialsException) {
                errorCode = ErrorCode.INVALID_CREDENTIALS;
            } else {
                errorCode = ErrorCode.AUTHENTICATION_FAILED;
            }
        }

        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        response.getWriter().write(
                DataSerializer.serialize(new ExceptionResponse(errorCode, data))
        );
    }

    private static Object putSuspendedData(UserSuspendedException suspendedEx) {
        Map<String, Object> map = new HashMap<>();
        map.put("reason", suspendedEx.getReason() != null ? suspendedEx.getReason().getDescription() : "사유 없음");
        map.put("suspendedUntil", suspendedEx.getSuspendedUntil());
        map.put("permanent", suspendedEx.isPermanent());

        return map;
    }

    private static Object putWithdrawalPendingData(WithdrawalPendingException ex) {
        Map<String, Object> map = new HashMap<>();
        map.put("scheduledDeletionAt", ex.getScheduledDeletionAt());
        map.put("remainingDays", ex.getRemainingDays());
        map.put("canRestore", ex.isCanRestore());

        return map;
    }

    private void loginFail(String email) {
        if (email != null) {
            loginAttemptService.loginFailed(email);
        }
    }
}
