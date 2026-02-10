package com.example.my_project_1.user.domain;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserDormancy {
    private static final int DORMANT_MONTHS = 12;

    private LocalDateTime lastActiveAt;

    public static UserDormancy from(LocalDateTime lastLoginAt) {
        UserDormancy userDormancy = new UserDormancy();
        userDormancy.lastActiveAt = lastLoginAt;
        return userDormancy;
    }

    public boolean shouldDormant(LocalDateTime now) {
        return lastActiveAt != null && lastActiveAt.isBefore(now.minusMonths(DORMANT_MONTHS));
    }
}
