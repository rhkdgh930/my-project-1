package com.example.my_project_1.auth.cache;

import com.example.my_project_1.user.domain.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CachedUserContext {
    private Long userId;
    private String email;
    private Role role;
    private UserStatus userStatus;
    private AccountStatus accountStatus;

    private SuspensionReason reason;
    private LocalDateTime suspendedUntil;

    private LocalDateTime scheduledDeletionAt;
    private Long remainingDays;
    private boolean canRestore;

    private boolean deleted;

    public static CachedUserContext from(User user, LocalDateTime now) {

        UserWithdrawal withdrawal = user.getWithdrawal();

        LocalDateTime scheduledDeletionAt = null;
        Long remainingDays = null;
        boolean canRestore = false;

        if (withdrawal != null) {
            scheduledDeletionAt = withdrawal.getScheduledDeletionAt();
            remainingDays = withdrawal.getRemainingDays(now);
            canRestore = withdrawal.canRestore(now);
        }

        UserSuspension suspension = user.getSuspension();

        SuspensionReason suspensionReason = null;
        LocalDateTime suspendedUntil = null;

        if (suspension != null) {
            suspensionReason = suspension.getReason();
            suspendedUntil = suspension.getSuspendedUntil();
        }

        return new CachedUserContext(
                user.getId(),
                user.getEmail().getValue(),
                user.getRole(),
                user.getUserStatus(),
                user.getAccountStatus(),
                suspensionReason,
                suspendedUntil,
                scheduledDeletionAt,
                remainingDays,
                canRestore,
                user.isDeleted()
        );
    }
}
