package com.example.my_project_1.user.event;

import com.example.my_project_1.auth.service.EmailService;
import com.example.my_project_1.auth.service.RedisEmailVerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailEventListener {

    private final RedisEmailVerificationService redisEmailVerificationService;
    private final EmailService emailService;

    @Async("asyncTaskExecutor") // 별도의 스레드에서 실행됨
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleEmailVerification(EmailVerificationEvent event) {
        log.info("Sending verification email asynchronously to: {}", event.getEmail());
        try {
            redisEmailVerificationService.sendCode(event.getEmail());
        } catch (Exception e) {
            log.error("Failed to send verification email", e);
        }
    }

    @Async("asyncTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePasswordReset(PasswordResetEvent event) {
        log.info("Sending password reset link asynchronously to: {}", event.getEmail());
        try {
            emailService.sendPasswordResetLink(event.getEmail(), event.getResetLink());
        } catch (Exception e) {
            log.error("Failed to send password reset link", e);
        }
    }
}
