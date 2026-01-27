package com.example.my_project_1.user.service;

import com.example.my_project_1.user.domain.SuspensionReason;

public interface AdminCommandService {
    void suspendUser(Long userId, SuspensionReason reason);
}
