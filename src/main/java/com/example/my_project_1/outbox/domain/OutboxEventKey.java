package com.example.my_project_1.outbox.domain;

import com.example.my_project_1.user.event.UserAccountChangedType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class OutboxEventKey {

    private static final String DELIMITER = ":";

    public static String postCreated(Long postId) {
        return join(OutboxEventType.POST_CREATED.name(), postId);
    }

    public static String postUpdated(Long postId) {
        return join(OutboxEventType.POST_UPDATED.name(), postId, UUID.randomUUID());
    }

    public static String postDeleted(Long postId) {
        return join(OutboxEventType.POST_DELETED.name(), postId);
    }

    public static String dormancyNotify(Long userId, LocalDate lastLoginDate) {
        return join(OutboxEventType.DORMANCY_NOTIFY.name(), userId, lastLoginDate);
    }

    public static String userAccountChanged(Long userId, UserAccountChangedType type) {
        return join(OutboxEventType.USER_ACCOUNT_CHANGED.name(), userId, type.name(), UUID.randomUUID());
    }

    private static String join(Object... parts) {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                builder.append(DELIMITER);
            }
            builder.append(parts[i]);
        }

        return builder.toString();
    }
}
