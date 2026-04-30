package com.example.my_project_1.user.domain;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.Assert;

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
        Assert.notNull(type, "차단 유형은 필수입니다.");
        Assert.notNull(reason, "차단 사유는 필수입니다.");
        Assert.notNull(now, "차단 시작 시간은 필수입니다.");

        if (type == SuspensionType.TEMPORARY) {
            Assert.notNull(duration, "일시 차단 기간은 필수입니다.");
            Assert.isTrue(!duration.isZero() && !duration.isNegative(), "일시 차단 기간은 0보다 커야 합니다.");
        }

        UserSuspension suspension = new UserSuspension();
        suspension.type = type;
        suspension.reason = reason;
        suspension.suspendedAt = now;
        suspension.suspendedUntil = type == SuspensionType.PERMANENT ? null : now.plus(duration);
        return suspension;
    }


    public UserSuspension mergeWith(SuspensionType newType, SuspensionReason newReason, LocalDateTime now, Duration duration) {
        if (this.type == SuspensionType.PERMANENT) return this;
        if (newType == SuspensionType.PERMANENT) return create(newType, newReason, now, null);

        // 기존 정지가 끝나기 전이라면, 기존 종료일 기준으로 연장. 끝났다면 현재 시간 기준.
        LocalDateTime baseTime = (this.suspendedUntil != null && this.suspendedUntil.isAfter(now))
                ? this.suspendedUntil : now;

        return create(newType, newReason, now, Duration.between(now, baseTime.plus(duration)));
    }

    public boolean isActive(LocalDateTime now) {
        if (type == SuspensionType.PERMANENT) return true;
        return suspendedUntil != null && suspendedUntil.isAfter(now);
    }
}