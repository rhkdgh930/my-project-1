package com.example.my_project_1.user.listener;

import com.example.my_project_1.auth.service.EmailService;
import com.example.my_project_1.auth.service.RedisEmailVerificationService;
import com.example.my_project_1.user.event.EmailVerificationMailRequestedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class EmailVerificationMailEventListenerTest {

    private static final String EMAIL = "email@email.com";
    private static final String CODE = "123456";

    @Test
    @DisplayName("이메일 인증 메일 이벤트를 받으면 인증 코드를 이메일로 발송한다.")
    void sendVerificationMail_sendsEmail() {
        EmailService emailService = mock(EmailService.class);
        RedisEmailVerificationService redisEmailVerificationService =
                mock(RedisEmailVerificationService.class);
        EmailVerificationMailEventListener listener =
                new EmailVerificationMailEventListener(emailService, redisEmailVerificationService);

        listener.sendVerificationMail(event());

        verify(emailService).sendVerificationCode(EMAIL, CODE);
        verifyNoInteractions(redisEmailVerificationService);
    }

    @Test
    @DisplayName("이메일 인증 메일 발송이 실패하면 Redis 인증 코드를 삭제한다.")
    void sendVerificationMail_deletesRedisCodeWhenEmailSendFails() {
        EmailService emailService = mock(EmailService.class);
        RedisEmailVerificationService redisEmailVerificationService =
                mock(RedisEmailVerificationService.class);
        RuntimeException emailFailure = new RuntimeException("email send failed");
        doThrow(emailFailure).when(emailService).sendVerificationCode(EMAIL, CODE);
        EmailVerificationMailEventListener listener =
                new EmailVerificationMailEventListener(emailService, redisEmailVerificationService);

        listener.sendVerificationMail(event());

        verify(emailService).sendVerificationCode(EMAIL, CODE);
        verify(redisEmailVerificationService).deleteCode(EMAIL);
        verifyNoMoreInteractions(redisEmailVerificationService);
    }

    private EmailVerificationMailRequestedEvent event() {
        return new EmailVerificationMailRequestedEvent(EMAIL, CODE);
    }
}
