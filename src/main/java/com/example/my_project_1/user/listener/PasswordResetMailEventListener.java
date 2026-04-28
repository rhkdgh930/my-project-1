package com.example.my_project_1.user.listener;

import com.example.my_project_1.auth.service.EmailService;
import com.example.my_project_1.auth.service.RedisPasswordResetTokenService;
import com.example.my_project_1.user.event.PasswordResetMailRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class PasswordResetMailEventListener {

    private final EmailService emailService;
    private final RedisPasswordResetTokenService redisPasswordResetTokenService;

    @Async("asyncTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendPasswordResetMail(PasswordResetMailRequestedEvent event) {
        try {
            emailService.sendPasswordResetLink(event.getEmail(), event.getResetLink());
        } catch (RuntimeException e) {
            redisPasswordResetTokenService.deleteToken(event.getRawToken());
            log.error(
                    "[USER][PASSWORD_RESET_MAIL][SEND_FAIL] email={} errorType={}",
                    event.getEmail(),
                    e.getClass().getSimpleName()
            );
        }
    }
}
