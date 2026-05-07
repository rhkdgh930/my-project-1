package com.example.my_project_1.user.service.impl;

import com.example.my_project_1.auth.service.RedisEmailVerificationService;
import com.example.my_project_1.auth.service.RedisPasswordResetTokenService;
import com.example.my_project_1.outbox.service.UserAccountChangeOutboxPublisher;
import com.example.my_project_1.user.domain.Email;
import com.example.my_project_1.user.domain.User;
import com.example.my_project_1.user.event.EmailVerificationMailRequestedEvent;
import com.example.my_project_1.user.event.PasswordResetMailRequestedEvent;
import com.example.my_project_1.user.event.UserAccountChangedType;
import com.example.my_project_1.user.repository.UserRepository;
import com.example.my_project_1.user.service.request.PasswordResetRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class UserCommandServiceImplTest {

    private static final String FRONTEND_URL = "https://frontend.example";
    private static final String EMAIL = "email@email.com";

    private final Clock clock = Clock.fixed(
            Instant.parse("2026-01-01T00:00:00Z"),
            ZoneId.of("UTC")
    );

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private RedisEmailVerificationService redisEmailVerificationService;
    private RedisPasswordResetTokenService redisPasswordResetTokenService;
    private UserAccountChangeOutboxPublisher userAccountChangeOutboxPublisher;
    private ApplicationEventPublisher eventPublisher;
    private UserCommandServiceImpl userCommandService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        redisEmailVerificationService = mock(RedisEmailVerificationService.class);
        redisPasswordResetTokenService = mock(RedisPasswordResetTokenService.class);
        userAccountChangeOutboxPublisher = mock(UserAccountChangeOutboxPublisher.class);
        eventPublisher = mock(ApplicationEventPublisher.class);

        userCommandService = new UserCommandServiceImpl(
                clock,
                userRepository,
                passwordEncoder,
                redisEmailVerificationService,
                redisPasswordResetTokenService,
                userAccountChangeOutboxPublisher,
                eventPublisher
        );
        ReflectionTestUtils.setField(userCommandService, "frontendUrl", FRONTEND_URL);
    }

    @Test
    @DisplayName("이메일 인증 코드 요청은 Redis에 code를 저장한 뒤 메일 발송 이벤트를 발행한다.")
    void sendVerificationCode_savesRedisCodeAndPublishesMailEvent() {
        when(userRepository.existsByEmail(Email.from(EMAIL))).thenReturn(false);

        userCommandService.sendVerificationCode(EMAIL);

        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<EmailVerificationMailRequestedEvent> eventCaptor =
                ArgumentCaptor.forClass(EmailVerificationMailRequestedEvent.class);

        verify(redisEmailVerificationService).saveCode(eq(EMAIL), codeCaptor.capture());
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        EmailVerificationMailRequestedEvent event = eventCaptor.getValue();
        assertThat(event.getEmail()).isEqualTo(EMAIL);
        assertThat(event.getCode()).isEqualTo(codeCaptor.getValue());
        assertThat(event.getCode()).hasSize(6);
    }

    @Test
    @DisplayName("비밀번호 재설정 요청은 Redis token 저장 후 메일 발송 이벤트를 발행한다.")
    void requestPasswordReset_savesRedisTokenAndPublishesMailEvent() {
        when(userRepository.findByEmail(Email.from(EMAIL)))
                .thenReturn(Optional.of(activeUser()));

        userCommandService.requestPasswordReset(EMAIL);

        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<PasswordResetMailRequestedEvent> eventCaptor =
                ArgumentCaptor.forClass(PasswordResetMailRequestedEvent.class);

        verify(redisPasswordResetTokenService).saveToken(tokenCaptor.capture(), eq(EMAIL));
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        PasswordResetMailRequestedEvent event = eventCaptor.getValue();
        assertThat(event.getEmail()).isEqualTo(EMAIL);
        assertThat(event.getRawToken()).isEqualTo(tokenCaptor.getValue());
        assertThat(event.getResetLink())
                .isEqualTo(FRONTEND_URL + "/password-reset?token=" + tokenCaptor.getValue());
    }

    @Test
    @DisplayName("존재하지 않는 이메일의 비밀번호 재설정 요청은 token과 event를 만들지 않는다.")
    void requestPasswordReset_doesNothingWhenUserNotFound() {
        when(userRepository.findByEmail(Email.from(EMAIL)))
                .thenReturn(Optional.empty());

        userCommandService.requestPasswordReset(EMAIL);

        verifyNoInteractions(redisPasswordResetTokenService, eventPublisher);
    }

    @Test
    @DisplayName("비밀번호 재설정은 Redis token을 원자 소비한 뒤 비밀번호를 변경하고 token을 다시 삭제하지 않는다.")
    void resetPassword_consumesTokenAndDoesNotDeleteTokenAgain() {
        String rawToken = "reset-token";
        String newPassword = "newPassword123!";
        String encodedPassword = "encodedNewPassword";
        User user = activeUser();
        ReflectionTestUtils.setField(user, "id", 1L);
        PasswordResetRequest request = new PasswordResetRequest();
        ReflectionTestUtils.setField(request, "token", rawToken);
        ReflectionTestUtils.setField(request, "newPassword", newPassword);

        when(redisPasswordResetTokenService.consumeToken(rawToken)).thenReturn(EMAIL);
        when(userRepository.findByEmail(Email.from(EMAIL))).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(newPassword)).thenReturn(encodedPassword);

        userCommandService.resetPassword(request);

        verify(redisPasswordResetTokenService).consumeToken(rawToken);
        verify(redisPasswordResetTokenService, never()).validateAndGetEmail(rawToken);
        verify(redisPasswordResetTokenService, never()).deleteToken(rawToken);
        verify(userAccountChangeOutboxPublisher).publish(1L, UserAccountChangedType.SECURITY_CHANGED);
        assertThat(user.getPassword()).isEqualTo(encodedPassword);
    }

    private User activeUser() {
        return User.signUp(
                Email.from(EMAIL),
                "encodedPassword",
                "nickname",
                LocalDateTime.now(clock)
        );
    }
}
