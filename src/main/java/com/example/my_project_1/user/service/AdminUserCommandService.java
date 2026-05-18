package com.example.my_project_1.user.service;

import com.example.my_project_1.user.domain.SuspensionReason;
import com.example.my_project_1.user.domain.SuspensionType;

import java.time.Duration;

public interface AdminUserCommandService {
    void suspendUser(Long userId, SuspensionType type, SuspensionReason reason, Duration duration);

    void unSuspendUser(Long userId);

    void suspendUserByAdmin(Long adminId, Long userId, SuspensionType type, SuspensionReason reason, Duration duration);

    void unSuspendUserByAdmin(Long adminId, Long userId);
}
