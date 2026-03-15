package com.example.my_project_1.user.event.listener;

import com.example.my_project_1.auth.service.EmailService;
import com.example.my_project_1.auth.service.RedisEmailVerificationService;
import com.example.my_project_1.user.event.EmailVerificationEvent;
import com.example.my_project_1.user.event.PasswordResetEvent;
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

    @Async("asyncTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleEmailVerification(EmailVerificationEvent event) {
        log.info(
                "[EVENT][EmailVerificationListener][SEND_START] email={}",
                event.getEmail()
        );
        try {
            redisEmailVerificationService.sendCode(event.getEmail());
        } catch (Exception e) {
            log.error(
                    "[EVENT][EmailVerificationListener][SEND_FAIL] email={}",
                    event.getEmail(),
                    e
            );
        }
    }

    @Async("asyncTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePasswordReset(PasswordResetEvent event) {
        log.info(
                "[EVENT][PasswordResetListener][SEND_START] email={}",
                event.getEmail()
        );
        try {
            emailService.sendPasswordResetLink(event.getEmail(), event.getResetLink());
        } catch (Exception e) {
            log.error(
                    "[EVENT][PasswordResetListener][SEND_FAIL] email={}",
                    event.getEmail(),
                    e
            );
        }
    }
}
