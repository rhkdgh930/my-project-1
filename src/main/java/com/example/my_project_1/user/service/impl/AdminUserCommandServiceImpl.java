package com.example.my_project_1.user.service.impl;

import com.example.my_project_1.admin.domain.AdminActionTargetType;
import com.example.my_project_1.admin.domain.AdminActionType;
import com.example.my_project_1.admin.service.AdminActionLogService;
import com.example.my_project_1.common.exception.CustomException;
import com.example.my_project_1.common.exception.ErrorCode;
import com.example.my_project_1.outbox.service.UserAccountChangeOutboxPublisher;
import com.example.my_project_1.user.domain.SuspensionReason;
import com.example.my_project_1.user.domain.SuspensionType;
import com.example.my_project_1.user.domain.User;
import com.example.my_project_1.user.event.UserAccountChangedType;
import com.example.my_project_1.user.repository.UserRepository;
import com.example.my_project_1.user.service.AdminUserCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Transactional
@Service
@RequiredArgsConstructor
public class AdminUserCommandServiceImpl implements AdminUserCommandService {
    private final Clock clock;

    private final UserRepository userRepository;
    private final UserAccountChangeOutboxPublisher userAccountChangeOutboxPublisher;
    private final AdminActionLogService adminActionLogService;

    @Override
    public void suspendUser(Long userId, SuspensionType type, SuspensionReason reason, Duration duration) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        user.suspend(type, reason, duration, LocalDateTime.now(clock));

        userAccountChangeOutboxPublisher.publish(userId, UserAccountChangedType.SECURITY_CHANGED);
    }

    @Override
    public void unSuspendUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        user.unSuspend();
        userAccountChangeOutboxPublisher.publish(userId, UserAccountChangedType.SECURITY_CHANGED);
    }

    @Override
    public void suspendUserByAdmin(Long adminId, Long userId, SuspensionType type, SuspensionReason reason, Duration duration) {
        suspendUser(userId, type, reason, duration);
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("type", type.name());
        metadata.put("reason", reason.name());
        metadata.put("days", duration == null ? null : duration.toDays());
        adminActionLogService.log(
                adminId,
                AdminActionType.USER_SUSPEND,
                AdminActionTargetType.USER,
                userId,
                "관리자가 유저를 정지했습니다.",
                metadata
        );
    }

    @Override
    public void unSuspendUserByAdmin(Long adminId, Long userId) {
        unSuspendUser(userId);
        adminActionLogService.log(
                adminId,
                AdminActionType.USER_UNSUSPEND,
                AdminActionTargetType.USER,
                userId,
                "관리자가 유저 정지를 해제했습니다.",
                Map.of()
        );
    }
}
