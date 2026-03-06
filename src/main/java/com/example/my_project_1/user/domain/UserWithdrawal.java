package com.example.my_project_1.user.domain;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

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

    public LocalDateTime getScheduledDeletionAt() {
        if (!isPending()) return null;
        return requestedAt.plusDays(RETENTION_DAYS);
    }

    public Long getRemainingDays(LocalDateTime now) {
        if (!isPending()) return null;
        return ChronoUnit.DAYS.between(
                now.toLocalDate(),
                getScheduledDeletionAt().toLocalDate()
        );
    }

    public boolean canRestore(LocalDateTime now) {
        if (!isPending()) return false;
        return now.isBefore(getScheduledDeletionAt());
    }

    public boolean shouldDelete(LocalDateTime now) {
        if (!isPending()) return false;
        return now.isAfter(getScheduledDeletionAt());
    }
}
