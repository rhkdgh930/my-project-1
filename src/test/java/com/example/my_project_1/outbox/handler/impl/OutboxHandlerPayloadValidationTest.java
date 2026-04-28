package com.example.my_project_1.outbox.handler.impl;

import com.example.my_project_1.auth.service.EmailService;
import com.example.my_project_1.auth.service.RedisEmailVerificationService;
import com.example.my_project_1.auth.service.RedisPasswordResetTokenService;
import com.example.my_project_1.auth.service.RedisTokenService;
import com.example.my_project_1.auth.service.RedisUserContextService;
import com.example.my_project_1.common.utils.DataSerializer;
import com.example.my_project_1.image.service.ImageService;
import com.example.my_project_1.post.event.PostCreatedOutboxEvent;
import com.example.my_project_1.post.event.PostUpdatedOutboxEvent;
import com.example.my_project_1.user.event.DormancyNotifyOutboxEvent;
import com.example.my_project_1.user.event.EmailVerificationOutboxEvent;
import com.example.my_project_1.user.event.PasswordResetOutboxEvent;
import com.example.my_project_1.user.event.UserAccountChangedOutboxEvent;
import com.example.my_project_1.user.event.UserAccountChangedType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class OutboxHandlerPayloadValidationTest {

    @Test
    @DisplayName("USER_ACCOUNT_CHANGED payload의 userId가 null이면 Redis side effect를 실행하지 않는다.")
    void userAccountChanged_rejectsNullUserIdBeforeSideEffect() {
        RedisUserContextService redisUserContextService = mock(RedisUserContextService.class);
        RedisTokenService redisTokenService = mock(RedisTokenService.class);
        UserAccountChangedHandler handler =
                new UserAccountChangedHandler(redisUserContextService, redisTokenService);

        String payload = DataSerializer.serialize(
                new UserAccountChangedOutboxEvent(null, UserAccountChangedType.SECURITY_CHANGED)
        );

        assertThatThrownBy(() -> handler.handle(payload))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(redisUserContextService, redisTokenService);
    }

    @Test
    @DisplayName("DORMANCY_NOTIFY payload의 email이 null이면 email side effect를 실행하지 않는다.")
    void dormancyNotify_rejectsNullEmailBeforeSideEffect() {
        EmailService emailService = mock(EmailService.class);
        DormancyNotifyHandler handler = new DormancyNotifyHandler(emailService);

        String payload = DataSerializer.serialize(
                new DormancyNotifyOutboxEvent(1L, null, "nickname")
        );

        assertThatThrownBy(() -> handler.handle(payload))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("EMAIL_VERIFICATION payload의 email이 null이면 Redis/email side effect를 실행하지 않는다.")
    void emailVerification_rejectsNullEmailBeforeSideEffect() {
        RedisEmailVerificationService redisEmailVerificationService = mock(RedisEmailVerificationService.class);
        EmailService emailService = mock(EmailService.class);
        EmailVerificationHandler handler =
                new EmailVerificationHandler(redisEmailVerificationService, emailService);

        String payload = DataSerializer.serialize(
                new EmailVerificationOutboxEvent(null, "123456")
        );

        assertThatThrownBy(() -> handler.handle(payload))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(redisEmailVerificationService, emailService);
    }

    @Test
    @DisplayName("PASSWORD_RESET payload의 rawToken이 null이면 Redis/email side effect를 실행하지 않는다.")
    void passwordReset_rejectsNullRawTokenBeforeSideEffect() {
        RedisPasswordResetTokenService redisPasswordResetTokenService = mock(RedisPasswordResetTokenService.class);
        EmailService emailService = mock(EmailService.class);
        PasswordResetHandler handler =
                new PasswordResetHandler(redisPasswordResetTokenService, emailService);

        String payload = DataSerializer.serialize(
                new PasswordResetOutboxEvent("email@email.com", null, "https://example.com/reset")
        );

        assertThatThrownBy(() -> handler.handle(payload))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(redisPasswordResetTokenService, emailService);
    }

    @Test
    @DisplayName("POST_CREATED payload의 storageKeys가 빈 목록이면 기존 정책대로 image service를 호출할 수 있다.")
    void postCreated_allowsEmptyStorageKeys() {
        ImageService imageService = mock(ImageService.class);
        PostCreatedHandler handler = new PostCreatedHandler(imageService);

        String payload = DataSerializer.serialize(
                new PostCreatedOutboxEvent(1L, 2L, List.of())
        );

        handler.handle(payload);
    }
}
