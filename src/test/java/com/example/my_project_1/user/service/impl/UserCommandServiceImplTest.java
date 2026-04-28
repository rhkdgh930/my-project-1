package com.example.my_project_1.user.service.impl;

import com.example.my_project_1.auth.service.RedisEmailVerificationService;
import com.example.my_project_1.auth.service.RedisPasswordResetTokenService;
import com.example.my_project_1.outbox.service.OutboxPublisher;
import com.example.my_project_1.outbox.service.UserAccountChangeOutboxPublisher;
import com.example.my_project_1.user.domain.Email;
import com.example.my_project_1.user.domain.User;
import com.example.my_project_1.user.event.PasswordResetMailRequestedEvent;
import com.example.my_project_1.user.repository.UserRepository;
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
    private RedisPasswordResetTokenService redisPasswordResetTokenService;
    private OutboxPublisher outboxPublisher;
    private ApplicationEventPublisher eventPublisher;
    private UserCommandServiceImpl userCommandService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        RedisEmailVerificationService redisEmailVerificationService = mock(RedisEmailVerificationService.class);
        redisPasswordResetTokenService = mock(RedisPasswordResetTokenService.class);
        outboxPublisher = mock(OutboxPublisher.class);
        UserAccountChangeOutboxPublisher userAccountChangeOutboxPublisher =
                mock(UserAccountChangeOutboxPublisher.class);
        eventPublisher = mock(ApplicationEventPublisher.class);

        userCommandService = new UserCommandServiceImpl(
                clock,
                userRepository,
                passwordEncoder,
                redisEmailVerificationService,
                redisPasswordResetTokenService,
                outboxPublisher,
                userAccountChangeOutboxPublisher,
                eventPublisher
        );
        ReflectionTestUtils.setField(userCommandService, "frontendUrl", FRONTEND_URL);
    }

    @Test
    @DisplayName("비밀번호 재설정 요청은 Redis token 저장 후 메일 발송 이벤트를 발행하고 Outbox를 사용하지 않는다.")
    void requestPasswordReset_savesRedisTokenAndPublishesMailEventWithoutOutbox() {
        when(userRepository.findByEmail(Email.from(EMAIL)))
                .thenReturn(Optional.of(activeUser()));

        userCommandService.requestPasswordReset(EMAIL);

        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<PasswordResetMailRequestedEvent> eventCaptor =
                ArgumentCaptor.forClass(PasswordResetMailRequestedEvent.class);

        verify(redisPasswordResetTokenService).saveToken(tokenCaptor.capture(), eq(EMAIL));
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        verifyNoInteractions(outboxPublisher);

        PasswordResetMailRequestedEvent event = eventCaptor.getValue();
        assertThat(event.getEmail()).isEqualTo(EMAIL);
        assertThat(event.getRawToken()).isEqualTo(tokenCaptor.getValue());
        assertThat(event.getResetLink())
                .isEqualTo(FRONTEND_URL + "/password-reset?token=" + tokenCaptor.getValue());
    }

    @Test
    @DisplayName("존재하지 않는 이메일의 비밀번호 재설정 요청은 token, event, Outbox side effect를 만들지 않는다.")
    void requestPasswordReset_doesNothingWhenUserNotFound() {
        when(userRepository.findByEmail(Email.from(EMAIL)))
                .thenReturn(Optional.empty());

        userCommandService.requestPasswordReset(EMAIL);

        verifyNoInteractions(redisPasswordResetTokenService, eventPublisher, outboxPublisher);
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
