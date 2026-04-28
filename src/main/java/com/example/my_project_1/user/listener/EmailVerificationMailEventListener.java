package com.example.my_project_1.user.listener;

import com.example.my_project_1.auth.service.EmailService;
import com.example.my_project_1.auth.service.RedisEmailVerificationService;
import com.example.my_project_1.user.event.EmailVerificationMailRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailVerificationMailEventListener {

    private final EmailService emailService;
    private final RedisEmailVerificationService redisEmailVerificationService;

    @Async("asyncTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendVerificationMail(EmailVerificationMailRequestedEvent event) {
        try {
            emailService.sendVerificationCode(event.getEmail(), event.getCode());
        } catch (RuntimeException e) {
            redisEmailVerificationService.deleteCode(event.getEmail());
            log.error(
                    "[USER][EMAIL_VERIFICATION_MAIL][SEND_FAIL] email={} errorType={}",
                    event.getEmail(),
                    e.getClass().getSimpleName()
            );
        }
    }
}
