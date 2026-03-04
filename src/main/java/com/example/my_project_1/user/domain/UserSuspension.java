package com.example.my_project_1.user.domain;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSuspension {

    @Enumerated(EnumType.STRING)
    private SuspensionType type;

    @Enumerated(EnumType.STRING)
    private SuspensionReason reason;

    private LocalDateTime suspendedAt;
    private LocalDateTime suspendedUntil;

    public static UserSuspension create(SuspensionType type, SuspensionReason reason, LocalDateTime now, Duration duration) {
        UserSuspension suspension = new UserSuspension();
        suspension.type = type;
        suspension.reason = reason;
        suspension.suspendedAt = now;
        suspension.suspendedUntil = (type == SuspensionType.PERMANENT) ? null : now.plus(duration);
        return suspension;
    }

    public UserSuspension mergeWith(SuspensionType newType, SuspensionReason newReason, LocalDateTime now, Duration duration) {
        if (this.type == SuspensionType.PERMANENT) return this;

        if (newType == SuspensionType.PERMANENT) return create(newType, newReason, now, null);

        LocalDateTime newProposedUntil = now.plus(duration);
        LocalDateTime finalUntil = this.suspendedUntil.isAfter(newProposedUntil) ? this.suspendedUntil : newProposedUntil;

        return create(newType, newReason, now, Duration.between(now, finalUntil));
    }

    public boolean isActive(LocalDateTime now) {
        if (type == SuspensionType.PERMANENT) return true;
        return suspendedUntil != null && suspendedUntil.isAfter(now);
    }
}