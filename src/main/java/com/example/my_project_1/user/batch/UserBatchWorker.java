package com.example.my_project_1.user.batch;

import com.example.my_project_1.common.exception.CustomException;
import com.example.my_project_1.common.exception.ErrorCode;
import com.example.my_project_1.common.utils.DataSerializer;
import com.example.my_project_1.outbox.domain.OutboxEventKey;
import com.example.my_project_1.outbox.domain.OutboxEventType;
import com.example.my_project_1.outbox.repository.OutboxRepository;
import com.example.my_project_1.outbox.service.OutboxPublisher;
import com.example.my_project_1.outbox.service.UserAccountChangeOutboxPublisher;
import com.example.my_project_1.user.domain.User;
import com.example.my_project_1.user.domain.UserStatus;
import com.example.my_project_1.user.domain.UserWithdrawal;
import com.example.my_project_1.user.event.DormancyNotifyOutboxEvent;
import com.example.my_project_1.user.event.UserAccountChangedType;
import com.example.my_project_1.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserBatchWorker {

    private final OutboxPublisher outboxPublisher;
    private final UserAccountChangeOutboxPublisher userAccountChangeOutboxPublisher;
    private final UserRepository userRepository;
    private final OutboxRepository outboxRepository;

    @Transactional
    public void processSingleUserWithDormancy(Long userId, LocalDateTime notifyThreshold, LocalDateTime dormantThreshold) {
        User user = findUser(userId);
        LocalDateTime lastLoginAt = user.getLastLoginAt();

        if (!user.isActive() || lastLoginAt == null || lastLoginAt.isAfter(notifyThreshold)) {
            return;
        }

        if (lastLoginAt.isBefore(dormantThreshold)) {
            user.markDormant();
            userAccountChangeOutboxPublisher.publish(user.getId(), UserAccountChangedType.DORMANT_REQUEST);
            return;
        }

        publishDormancyNotifyIfNotExists(user);
    }

    private void publishDormancyNotifyIfNotExists(User user) {
        String eventKey = OutboxEventKey.dormancyNotify(user.getId(), user.getLastLoginAt().toLocalDate());

        if (outboxRepository.existsByEventKey(eventKey)) {
            return;
        }

        outboxPublisher.publish(
                OutboxEventType.DORMANCY_NOTIFY,
                DataSerializer.serialize(
                        new DormancyNotifyOutboxEvent(
                                user.getId(),
                                user.getEmail().getValue(),
                                user.getNickname()
                        )
                ),
                eventKey
        );
    }

    @Transactional
    public void processSingleWithdrawal(Long userId, LocalDateTime threshold) {
        User user = findUser(userId);

        if (user.getUserStatus() != UserStatus.WITHDRAWN_REQUESTED) {
            return;
        }

        UserWithdrawal withdrawal = user.getWithdrawal();
        if (withdrawal == null || withdrawal.getRequestedAt() == null || withdrawal.getRequestedAt().isAfter(threshold)) {
            return;
        }

        user.completeWithdrawal();
        userAccountChangeOutboxPublisher.publish(user.getId(), UserAccountChangedType.WITHDRAWAL_REQUEST);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}
