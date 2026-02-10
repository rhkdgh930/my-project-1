package com.example.my_project_1.user.domain;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserWithdrawal {

    private static final int RETENTION_DAYS = 7;

    private LocalDateTime requestedAt;

    public static UserWithdrawal request(LocalDateTime now) {
        UserWithdrawal withdrawal = new UserWithdrawal();
        withdrawal.requestedAt = now;
        return withdrawal;
    }

    public boolean isPending() {
        return requestedAt != null;
    }

    public LocalDateTime scheduledDeletionAt() {
        return requestedAt.plusDays(RETENTION_DAYS);
    }

    public boolean canRestore(LocalDateTime now) {
        return now.isBefore(scheduledDeletionAt());
    }

    public boolean shouldDelete(LocalDateTime now) {
        return now.isAfter(scheduledDeletionAt());
    }
}
