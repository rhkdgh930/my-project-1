package com.example.my_project_1.user.listener;

import com.example.my_project_1.auth.service.EmailService;
import com.example.my_project_1.auth.service.RedisPasswordResetTokenService;
import com.example.my_project_1.user.event.PasswordResetMailRequestedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyNoInteractions;

class PasswordResetMailEventListenerTest {

    private static final String EMAIL = "email@email.com";
    private static final String RAW_TOKEN = "raw-token";
    private static final String RESET_LINK = "https://frontend.example/password-reset?token=raw-token";

    @Test
    @DisplayName("비밀번호 재설정 메일 이벤트를 받으면 reset link를 이메일로 발송한다.")
    void sendPasswordResetMail_sendsEmail() {
        EmailService emailService = mock(EmailService.class);
        RedisPasswordResetTokenService redisPasswordResetTokenService =
                mock(RedisPasswordResetTokenService.class);
        PasswordResetMailEventListener listener =
                new PasswordResetMailEventListener(emailService, redisPasswordResetTokenService);

        listener.sendPasswordResetMail(event());

        verify(emailService).sendPasswordResetLink(EMAIL, RESET_LINK);
        verifyNoInteractions(redisPasswordResetTokenService);
    }

    @Test
    @DisplayName("비밀번호 재설정 메일 발송이 실패하면 Redis reset token을 삭제한다.")
    void sendPasswordResetMail_deletesRedisTokenWhenEmailSendFails() {
        EmailService emailService = mock(EmailService.class);
        RedisPasswordResetTokenService redisPasswordResetTokenService =
                mock(RedisPasswordResetTokenService.class);
        RuntimeException emailFailure = new RuntimeException("email send failed");
        doThrow(emailFailure).when(emailService).sendPasswordResetLink(EMAIL, RESET_LINK);
        PasswordResetMailEventListener listener =
                new PasswordResetMailEventListener(emailService, redisPasswordResetTokenService);

        listener.sendPasswordResetMail(event());

        verify(emailService).sendPasswordResetLink(EMAIL, RESET_LINK);
        verify(redisPasswordResetTokenService).deleteToken(RAW_TOKEN);
        verifyNoMoreInteractions(redisPasswordResetTokenService);
    }

    private PasswordResetMailRequestedEvent event() {
        return new PasswordResetMailRequestedEvent(EMAIL, RAW_TOKEN, RESET_LINK);
    }
}
