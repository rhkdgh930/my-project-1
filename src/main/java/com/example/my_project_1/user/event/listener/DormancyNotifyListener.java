package com.example.my_project_1.user.event.listener;

import com.example.my_project_1.auth.service.EmailService;
import com.example.my_project_1.user.event.DormancyNotifyEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class DormancyNotifyListener {
    private final EmailService emailService;

    @Async("asyncTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleDormancyNotify(DormancyNotifyEvent event) {
        try {
            emailService.sendDormancyWarning(
                    event.getEmail(),
                    event.getNickname()
            );
            log.info(
                    "[EVENT][DormancyNotifyListener][SEND_SUCCESS] userId={}",
                    event.getUserId()
            );
        } catch (Exception e) {
            log.error(
                    "[EVENT][DormancyNotifyListener][SEND_FAIL] userId={}",
                    event.getUserId(),
                    e
            );
        }
    }
}
