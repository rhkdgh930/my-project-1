package com.example.my_project_1.user.event;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UserAccountChangedEvent {
    private final Long userId;
    private final boolean isSecurityCritical;

    public static UserAccountChangedEvent profileUpdated(Long userId) {
        return new UserAccountChangedEvent(userId, false);
    }

    public static UserAccountChangedEvent securityStateChanged(Long userId) {
        return new UserAccountChangedEvent(userId, true);
    }
}
