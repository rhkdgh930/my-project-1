package com.example.my_project_1.auth.exception;

import com.example.my_project_1.user.domain.SuspensionReason;
import lombok.Getter;
import org.springframework.security.authentication.LockedException;

import java.time.LocalDateTime;

@Getter
public class UserSuspendedException extends LockedException {
    private final LocalDateTime suspendedUntil;
    private final SuspensionReason reason;

    public UserSuspendedException(String message, LocalDateTime suspendedUntil, SuspensionReason reason) {
        super(message);
        this.suspendedUntil = suspendedUntil;
        this.reason = reason;
    }

    public boolean isPermanent() {
        return suspendedUntil == null;
    }

}
