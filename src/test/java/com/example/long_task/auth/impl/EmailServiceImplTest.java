package com.example.long_task.auth.impl;

import com.example.my_project_1.auth.service.EmailService;
import com.example.my_project_1.auth.service.impl.EmailServiceImpl;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.lang.reflect.Method;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(EmailServiceImplTest.TestConfig.class)
class EmailServiceImplTest {

    private static final String EMAIL = "email@email.com";
    private static final String RESET_LINK = "https://frontend.example/password-reset?token=raw-token";
    private static final String CODE = "123456";

    @Autowired
    private EmailService emailService;

    @Autowired
    private JavaMailSender javaMailSender;

    @BeforeEach
    void setUp() {
        reset(javaMailSender);
    }

    @Test
    @DisplayName("메일 발송 실패는 MailSendException으로 변환되어 caller에게 전파된다.")
    void sendVerificationCode_convertsMailFailureToMailSendException() {
        mockMimeMessage();
        doThrow(new MailSendException("smtp failed " + CODE))
                .when(javaMailSender).send(any(MimeMessage.class));

        assertThatThrownBy(() -> emailService.sendVerificationCode(EMAIL, CODE))
                .isInstanceOf(MailSendException.class)
                .hasMessage("email send failed");
    }

    @Test
    @DisplayName("MailSendException은 기존 정책대로 3회 재시도 후 caller에게 전파된다.")
    void sendPasswordResetLink_retriesMailSendExceptionThreeTimes() {
        mockMimeMessage();
        doThrow(new MailSendException("smtp failed " + RESET_LINK))
                .when(javaMailSender).send(any(MimeMessage.class));

        assertThatThrownBy(() -> emailService.sendPasswordResetLink(EMAIL, RESET_LINK))
                .isInstanceOf(MailSendException.class)
                .hasMessage("email send failed");

        verify(javaMailSender, times(3)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("@Recover는 세 public 메서드의 인자 구조와 맞는 명시적 시그니처를 사용한다.")
    void recoverMethod_usesExplicitTwoStringArgumentSignature() throws NoSuchMethodException {
        Method recover = EmailServiceImpl.class.getDeclaredMethod(
                "recover",
                MailSendException.class,
                String.class,
                String.class
        );

        assertThat(recover.getReturnType()).isEqualTo(void.class);
    }

    private void mockMimeMessage() {
        when(javaMailSender.createMimeMessage())
                .thenReturn(new MimeMessage(Session.getInstance(new Properties())));
    }

    @Configuration
    @EnableRetry
    static class TestConfig {

        @Bean
        JavaMailSender javaMailSender() {
            return mock(JavaMailSender.class);
        }

        @Bean
        EmailServiceImpl emailService(JavaMailSender javaMailSender) {
            return new EmailServiceImpl(javaMailSender);
        }
    }
}
