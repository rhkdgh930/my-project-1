package com.example.my_project_1.user.batch;

import com.example.my_project_1.common.utils.DataSerializer;
import com.example.my_project_1.common.exception.CustomException;
import com.example.my_project_1.common.exception.ErrorCode;
import com.example.my_project_1.outbox.domain.OutboxEventType;
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

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserBatchWorker {
    private final Clock clock;
    private static final String DORMANCY_NOTIFY = "DORMANCY_NOTIFY:";

    private final OutboxPublisher outboxPublisher;
    private final UserAccountChangeOutboxPublisher userAccountChangeOutboxPublisher;
    private final UserRepository userRepository;

    @Transactional
    public void processSingleUserWithDormancy(Long userId, LocalDateTime notifyThreshold, LocalDateTime dormantThreshold) {
        User user = findUser(userId);

        if (!user.isActive() || user.getLastLoginAt().isAfter(notifyThreshold)) {
            return;
        }

        if (user.getLastLoginAt().isBefore(dormantThreshold)) {
            user.markDormant();
            userAccountChangeOutboxPublisher.publish(user.getId(), UserAccountChangedType.DORMANT_REQUEST);
            return;
        }
        String eventKey = getEventKey(user);

        outboxPublisher.publish(OutboxEventType.DORMANCY_NOTIFY,
                DataSerializer.serialize(
                        new DormancyNotifyOutboxEvent(
                                user.getId(),
                                user.getEmail().getValue(),
                                user.getNickname()
                        )
                ),
                eventKey);
    }

    private String getEventKey(User user) {
        return DORMANCY_NOTIFY + user.getId() + ":" + LocalDate.now(clock);
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
