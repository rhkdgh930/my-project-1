package com.example.my_project_1.outbox.handler.impl;

import com.example.my_project_1.auth.service.EmailService;
import com.example.my_project_1.auth.service.RedisTokenService;
import com.example.my_project_1.auth.service.RedisUserContextService;
import com.example.my_project_1.common.utils.DataSerializer;
import com.example.my_project_1.image.domain.ImageOwnerType;
import com.example.my_project_1.image.service.ImageService;
import com.example.my_project_1.post.event.PostCreatedOutboxEvent;
import com.example.my_project_1.post.event.PostDeletedOutboxEvent;
import com.example.my_project_1.user.event.DormancyNotifyOutboxEvent;
import com.example.my_project_1.user.event.UserAccountChangedOutboxEvent;
import com.example.my_project_1.user.event.UserAccountChangedType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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
    @DisplayName("POST_CREATED payload의 storageKeys가 빈 목록이면 기존 정책대로 image service를 호출할 수 있다.")
    void postCreated_allowsEmptyStorageKeys() {
        ImageService imageService = mock(ImageService.class);
        PostCreatedHandler handler = new PostCreatedHandler(imageService);

        String payload = DataSerializer.serialize(
                new PostCreatedOutboxEvent(1L, 2L, List.of())
        );

        handler.handle(payload);
    }

    @Test
    @DisplayName("POST_DELETED handler는 빈 목록으로 image sync를 호출해 기존 이미지를 detach 대상으로 만든다.")
    void postDeleted_syncsImagesWithEmptyStorageKeys() {
        ImageService imageService = mock(ImageService.class);
        PostDeletedHandler handler = new PostDeletedHandler(imageService);

        String payload = DataSerializer.serialize(
                new PostDeletedOutboxEvent(1L, 2L)
        );

        handler.handle(payload);

        verify(imageService).syncImages(1L, ImageOwnerType.POST, List.of(), 2L);
    }
}
