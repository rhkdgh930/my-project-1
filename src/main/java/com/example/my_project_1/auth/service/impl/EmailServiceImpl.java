package com.example.my_project_1.auth.service.impl;

import com.example.my_project_1.auth.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender javaMailSender;

    @Retryable(retryFor = MailSendException.class, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    @Override
    public void sendVerificationCode(String toEmail, String code) {
        String title = "[My Project] 회원가입 인증 코드입니다.";
        String content = String.format(
                """
                <div style="background-color: #f6f7f8; padding: 20px;">
                    <div style="background-color: #ffffff; padding: 20px; border-radius: 5px; box-shadow: 0 0 10px rgba(0,0,0,0.1);">
                        <h2>이메일 인증</h2>
                        <p>아래 인증 코드를 입력하여 회원가입을 완료해주세요.</p>
                        <h1 style="color: #333; letter-spacing: 5px;">%s</h1>
                        <p>이 코드는 5분간 유효합니다.</p>
                    </div>
                </div>
                """, code);

        sendMail(toEmail, title, content);
    }

    @Retryable(retryFor = MailSendException.class, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    @Override
    public void sendPasswordResetLink(String toEmail, String code) {
        String title = "[My Project] 비밀번호 변경 링크입니다.";
        String content = String.format(
                """
                <div style="background-color: #f6f7f8; padding: 20px;">
                    <div style="background-color: #ffffff; padding: 20px; border-radius: 5px; box-shadow: 0 0 10px rgba(0,0,0,0.1);">
                        <h2>비밀번호 변경 링크</h2>
                        <p>비밀번호 변경을 원하시면 아래 링크를 클릭해주세요.</p>
                        <h1 style="color: #333; letter-spacing: 1px;">%s</h1>
                        <p>이 코드는 5분간 유효합니다.</p>
                    </div>
                </div>
                """, code);

        sendMail(toEmail, title, content);
    }

    @Retryable(retryFor = MailSendException.class, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    @Override
    public void sendDormancyWarning(String toEmail, String nickname) {
        String title = "[My Project] 계정이 휴면 상태로 전환될 예정입니다.";
        String content = String.format("""
                <h2>휴면 전환 안내</h2>
                <p>안녕하세요, <strong>%s</strong>님.</p>
                <p>장기간 로그인이 없어 1개월 후 계정이 휴면 상태로 전환될 예정입니다.</p>
                <p>서비스 이용을 원하시면 로그인해주세요.</p>
                """, nickname);

        sendMail(toEmail, title, content);
    }

    @Recover
    public void recover(MailSendException e, String toEmail, String... args) {
        log.error(
                "[SERVICE][EmailService][RETRY_EXHAUSTED] to={} errorType={} message={}",
                toEmail,
                e.getClass().getSimpleName(),
                e.getMessage(),
                e
        );
        // TODO: 필요 시 DB 실패 로그 저장
    }

    private void sendMail(String to, String title, String content) {
        MimeMessage message = javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(title);
            helper.setText(content, true);
            javaMailSender.send(message);
            log.info(
                    "[SERVICE][EmailService][SEND_SUCCESS] to={}",
                    to
            );
        } catch (MessagingException e) {
            log.error(
                    "[SERVICE][EmailService][SEND_FAIL] to={}",
                    to,
                    e
            );
            throw new MailSendException("email send failed", e);
        }
    }
}
