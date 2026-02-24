package com.example.my_project_1.user.event;

import com.example.my_project_1.auth.service.EmailService;
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

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleDormancyNotify(DormancyNotifyEvent event) {
        try {
            emailService.sendDormancyWarning(
                    event.getEmail(),
                    event.getNickname()
            );
            log.info("[Email] Sent dormancy warning to userId={}", event.getUserId());
        } catch (Exception e) {
            // 비동기 실행 중 에러는 메인 배치에 영향을 주지 않음. 로깅 후 모니터링 대상.
            log.error("[Email] Failed to send warning to userId={}", event.getUserId(), e);
        }
    }
}
