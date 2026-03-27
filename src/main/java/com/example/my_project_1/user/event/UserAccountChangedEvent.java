package com.example.my_project_1.user.event;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UserAccountChangedEvent {
    private final Long userId;
    private final UserAccountChangedType type;

    public static UserAccountChangedEvent profileUpdated(Long userId) {
        return new UserAccountChangedEvent(userId, UserAccountChangedType.PROFILE_UPDATED);
    }

    public static UserAccountChangedEvent dormantReleased(Long userId) {
        return new UserAccountChangedEvent(userId, UserAccountChangedType.DORMANT_RELEASED);
    }

    public static UserAccountChangedEvent dormantRequest(Long userId) {
        return new UserAccountChangedEvent(userId, UserAccountChangedType.DORMANT_REQUEST);
    }

    public static UserAccountChangedEvent withdrawalRestored(Long userId) {
        return new UserAccountChangedEvent(userId, UserAccountChangedType.WITHDRAWAL_RESTORED);
    }

    public static UserAccountChangedEvent withdrawalRequest(Long userId) {
        return new UserAccountChangedEvent(userId, UserAccountChangedType.WITHDRAWAL_REQUEST);
    }

    public static UserAccountChangedEvent securityStateChanged(Long userId) {
        return new UserAccountChangedEvent(userId, UserAccountChangedType.SECURITY_CHANGED);
    }

}
