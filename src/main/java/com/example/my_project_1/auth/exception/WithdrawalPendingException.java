package com.example.my_project_1.auth.exception;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class WithdrawalPendingException extends WithdrawalException {
    private final LocalDateTime scheduledDeletionAt;
    private final long remainingDays;
    private final boolean canRestore;

    public WithdrawalPendingException(
            String message,
            LocalDateTime scheduledDeletionAt,
            long remainingDays,
            boolean canRestore) {
        super(message);
        this.scheduledDeletionAt = scheduledDeletionAt;
        this.remainingDays = remainingDays;
        this.canRestore = canRestore;
    }
}
