package com.example.my_project_1.user.service.request;

import com.example.my_project_1.user.domain.SuspensionReason;
import com.example.my_project_1.user.domain.SuspensionType;
import lombok.Getter;

import java.time.Duration;

@Getter
public class UserSuspensionRequest {
    private SuspensionType type;
    private SuspensionReason reason;
    private Long days;

    public Duration getDuration() {
        if (type == SuspensionType.PERMANENT) {
            return Duration.ZERO;
        }
        return Duration.ofDays(days != null ? days : 0);
    }
}
