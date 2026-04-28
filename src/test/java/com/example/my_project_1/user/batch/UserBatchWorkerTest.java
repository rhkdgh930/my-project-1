package com.example.my_project_1.user.batch;

import com.example.my_project_1.outbox.domain.OutboxEventType;
import com.example.my_project_1.outbox.service.OutboxPublisher;
import com.example.my_project_1.outbox.service.UserAccountChangeOutboxPublisher;
import com.example.my_project_1.user.domain.Email;
import com.example.my_project_1.user.domain.User;
import com.example.my_project_1.user.domain.UserStatus;
import com.example.my_project_1.user.event.UserAccountChangedType;
import com.example.my_project_1.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserBatchWorkerTest {

    private final Clock clock = Clock.systemDefaultZone();
    private final OutboxPublisher outboxPublisher = mock(OutboxPublisher.class);
    private final UserAccountChangeOutboxPublisher userAccountChangeOutboxPublisher =
            mock(UserAccountChangeOutboxPublisher.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserBatchWorker worker = new UserBatchWorker(
            clock,
            outboxPublisher,
            userAccountChangeOutboxPublisher,
            userRepository
    );

    @Test
    @DisplayName("휴면 전환은 worker transaction 안에서 userId로 User를 다시 조회한 뒤 처리한다.")
    void processSingleUserWithDormancy_loadsUserByIdAndMarksDormant() {
        Long userId = 1L;
        LocalDateTime now = LocalDateTime.now(clock);
        User user = user(userId, now.minusYears(2));

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        worker.processSingleUserWithDormancy(userId, now.minusMonths(11), now.minusMonths(12));

        verify(userRepository).findById(userId);
        assertThat(user.getUserStatus()).isEqualTo(UserStatus.DORMANT);
        verify(userAccountChangeOutboxPublisher)
                .publish(userId, UserAccountChangedType.DORMANT_REQUEST);
        verify(outboxPublisher, never()).publish(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    @DisplayName("정상 휴면 알림 대상이면 DORMANCY_NOTIFY Outbox event를 발행한다.")
    void processSingleUserWithDormancy_publishesDormancyNotifyWhenEligible() {
        Long userId = 1L;
        LocalDateTime now = LocalDateTime.now(clock);
        User user = user(userId, now.minusMonths(11).minusDays(1));

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        worker.processSingleUserWithDormancy(userId, now.minusMonths(11), now.minusMonths(12));

        assertThat(user.getUserStatus()).isEqualTo(UserStatus.ACTIVE);
        verify(outboxPublisher).publish(
                eq(OutboxEventType.DORMANCY_NOTIFY),
                anyString(),
                org.mockito.ArgumentMatchers.startsWith("DORMANCY_NOTIFY:" + userId + ":")
        );
        verify(userAccountChangeOutboxPublisher, never()).publish(anyLong(), any());
    }

    @Test
    @DisplayName("lastLoginAt이 notifyThreshold와 같으면 현재 정책상 휴면 알림 대상이다.")
    void processSingleUserWithDormancy_publishesNotifyWhenLastLoginAtEqualsNotifyThreshold() {
        Long userId = 1L;
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime notifyThreshold = now.minusMonths(11);
        User user = user(userId, notifyThreshold);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        worker.processSingleUserWithDormancy(userId, notifyThreshold, now.minusMonths(12));

        assertThat(user.getUserStatus()).isEqualTo(UserStatus.ACTIVE);
        verify(outboxPublisher).publish(eq(OutboxEventType.DORMANCY_NOTIFY), anyString(), anyString());
        verify(userAccountChangeOutboxPublisher, never()).publish(anyLong(), any());
    }

    @Test
    @DisplayName("lastLoginAt이 dormantThreshold와 같으면 현재 정책상 dormant 전환이 아니라 휴면 알림 대상이다.")
    void processSingleUserWithDormancy_publishesNotifyWhenLastLoginAtEqualsDormantThreshold() {
        Long userId = 1L;
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime dormantThreshold = now.minusMonths(12);
        User user = user(userId, dormantThreshold);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        worker.processSingleUserWithDormancy(userId, now.minusMonths(11), dormantThreshold);

        assertThat(user.getUserStatus()).isEqualTo(UserStatus.ACTIVE);
        verify(outboxPublisher).publish(eq(OutboxEventType.DORMANCY_NOTIFY), anyString(), anyString());
        verify(userAccountChangeOutboxPublisher, never()).publish(anyLong(), any());
    }

    @Test
    @DisplayName("worker에서 다시 조회한 User가 더 이상 휴면 알림 대상이 아니면 Outbox를 발행하지 않는다.")
    void processSingleUserWithDormancy_skipsWhenUserLoggedInAfterBatchQuery() {
        Long userId = 1L;
        LocalDateTime now = LocalDateTime.now(clock);
        User user = user(userId, now);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        worker.processSingleUserWithDormancy(userId, now.minusMonths(11), now.minusMonths(12));

        assertThat(user.getUserStatus()).isEqualTo(UserStatus.ACTIVE);
        verify(userAccountChangeOutboxPublisher, never())
                .publish(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any());
        verify(outboxPublisher, never()).publish(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    @DisplayName("ACTIVE가 아닌 사용자는 휴면 알림이나 dormant 전환을 하지 않는다.")
    void processSingleUserWithDormancy_skipsWhenUserIsNotActive() {
        Long userId = 1L;
        LocalDateTime now = LocalDateTime.now(clock);
        User user = user(userId, now.minusYears(2));
        user.requestWithdrawal(now);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        worker.processSingleUserWithDormancy(userId, now.minusMonths(11), now.minusMonths(12));

        assertThat(user.getUserStatus()).isEqualTo(UserStatus.WITHDRAWN_REQUESTED);
        verify(userAccountChangeOutboxPublisher, never()).publish(anyLong(), any());
        verify(outboxPublisher, never()).publish(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("탈퇴 완료는 worker transaction 안에서 userId로 User를 다시 조회한 뒤 처리한다.")
    void processSingleWithdrawal_loadsUserByIdAndCompletesWithdrawal() {
        Long userId = 1L;
        LocalDateTime now = LocalDateTime.now(clock);
        User user = user(userId, now);
        user.requestWithdrawal(now.minusDays(8));

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        worker.processSingleWithdrawal(userId, now.minusDays(7));

        verify(userRepository).findById(userId);
        assertThat(user.getUserStatus()).isEqualTo(UserStatus.WITHDRAWN);
        verify(userAccountChangeOutboxPublisher)
                .publish(userId, UserAccountChangedType.WITHDRAWAL_REQUEST);
    }

    @Test
    @DisplayName("withdrawal.requestedAt이 threshold와 같으면 현재 정책상 탈퇴 완료 대상이다.")
    void processSingleWithdrawal_completesWhenRequestedAtEqualsThreshold() {
        Long userId = 1L;
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime threshold = now.minusDays(7);
        User user = user(userId, now);
        user.requestWithdrawal(threshold);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        worker.processSingleWithdrawal(userId, threshold);

        assertThat(user.getUserStatus()).isEqualTo(UserStatus.WITHDRAWN);
        verify(userAccountChangeOutboxPublisher)
                .publish(userId, UserAccountChangedType.WITHDRAWAL_REQUEST);
    }

    @Test
    @DisplayName("worker에서 다시 조회한 탈퇴 요청이 아직 threshold에 도달하지 않았으면 완료 처리하지 않는다.")
    void processSingleWithdrawal_skipsWhenWithdrawalRequestIsNewerThanThreshold() {
        Long userId = 1L;
        LocalDateTime now = LocalDateTime.now(clock);
        User user = user(userId, now);
        user.requestWithdrawal(now.minusDays(1));

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        worker.processSingleWithdrawal(userId, now.minusDays(7));

        assertThat(user.getUserStatus()).isEqualTo(UserStatus.WITHDRAWN_REQUESTED);
        verify(userAccountChangeOutboxPublisher, never())
                .publish(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("withdrawal 정보가 null이면 탈퇴 완료 처리하지 않는다.")
    void processSingleWithdrawal_skipsWhenWithdrawalIsNull() {
        Long userId = 1L;
        LocalDateTime now = LocalDateTime.now(clock);
        User user = user(userId, now);
        ReflectionTestUtils.setField(user, "userStatus", UserStatus.WITHDRAWN_REQUESTED);
        ReflectionTestUtils.setField(user, "withdrawal", null);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        worker.processSingleWithdrawal(userId, now.minusDays(7));

        assertThat(user.getUserStatus()).isEqualTo(UserStatus.WITHDRAWN_REQUESTED);
        verify(userAccountChangeOutboxPublisher, never()).publish(anyLong(), any());
    }

    private User user(Long id, LocalDateTime lastLoginAt) {
        User user = User.signUp(
                Email.from("email" + id + "@email.com"),
                "encodedPassword123!",
                "nickname" + id,
                lastLoginAt
        );
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
