package com.example.my_project_1.user.service.impl;

import com.example.my_project_1.admin.domain.AdminActionTargetType;
import com.example.my_project_1.admin.domain.AdminActionType;
import com.example.my_project_1.admin.service.AdminActionLogService;
import com.example.my_project_1.outbox.service.UserAccountChangeOutboxPublisher;
import com.example.my_project_1.user.domain.SuspensionReason;
import com.example.my_project_1.user.domain.SuspensionType;
import com.example.my_project_1.user.domain.User;
import com.example.my_project_1.user.event.UserAccountChangedType;
import com.example.my_project_1.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminUserCommandServiceImplTest {

    private final Clock clock = Clock.fixed(
            Instant.parse("2026-05-16T01:02:03Z"),
            ZoneId.of("Asia/Seoul")
    );
    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserAccountChangeOutboxPublisher outboxPublisher = mock(UserAccountChangeOutboxPublisher.class);
    private final AdminActionLogService adminActionLogService = mock(AdminActionLogService.class);
    private final AdminUserCommandServiceImpl service = new AdminUserCommandServiceImpl(
            clock,
            userRepository,
            outboxPublisher,
            adminActionLogService
    );

    @Test
    @DisplayName("관리자 유저 정지 성공 시 감사 로그를 저장한다.")
    void suspendUserByAdmin_writesAuditLog() {
        User user = mock(User.class);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));

        service.suspendUserByAdmin(1L, 2L, SuspensionType.TEMPORARY, SuspensionReason.SPAM, Duration.ofDays(7));

        verify(user).suspend(SuspensionType.TEMPORARY, SuspensionReason.SPAM, Duration.ofDays(7), LocalDateTime.now(clock));
        verify(outboxPublisher).publish(2L, UserAccountChangedType.SECURITY_CHANGED);
        verify(adminActionLogService).log(
                eq(1L),
                eq(AdminActionType.USER_SUSPEND),
                eq(AdminActionTargetType.USER),
                eq(2L),
                eq("관리자가 유저를 정지했습니다."),
                anyMap()
        );
    }

    @Test
    @DisplayName("관리자 유저 정지 해제 성공 시 감사 로그를 저장한다.")
    void unSuspendUserByAdmin_writesAuditLog() {
        User user = mock(User.class);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));

        service.unSuspendUserByAdmin(1L, 2L);

        verify(user).unSuspend();
        verify(outboxPublisher).publish(2L, UserAccountChangedType.SECURITY_CHANGED);
        verify(adminActionLogService).log(
                eq(1L),
                eq(AdminActionType.USER_UNSUSPEND),
                eq(AdminActionTargetType.USER),
                eq(2L),
                eq("관리자가 유저 정지를 해제했습니다."),
                anyMap()
        );
    }
}
