package com.example.my_project_1.user.batch;

import com.example.my_project_1.common.utils.DataSerializer;
import com.example.my_project_1.outbox.domain.OutboxEventType;
import com.example.my_project_1.outbox.service.OutboxPublisher;
import com.example.my_project_1.outbox.service.UserAccountChangeOutboxPublisher;
import com.example.my_project_1.user.domain.User;
import com.example.my_project_1.user.event.DormancyNotifyOutboxEvent;
import com.example.my_project_1.user.event.UserAccountChangedType;
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

    @Transactional
    public void processSingleUserWithDormancy(User user, LocalDateTime threshold) {
        if (user.getLastLoginAt().isBefore(threshold)) {
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
    public void processSingleWithdrawal(User user) {
        user.completeWithdrawal();
        userAccountChangeOutboxPublisher.publish(user.getId(), UserAccountChangedType.WITHDRAWAL_REQUEST);
    }
}
