package com.example.my_project_1.auth.service.impl;

import com.example.my_project_1.auth.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
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
        sendMail(
                toEmail,
                "[My Project] 이메일 인증 코드입니다",
                verificationCodeContent(code)
        );
    }

    @Retryable(retryFor = MailSendException.class, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    @Override
    public void sendPasswordResetLink(String toEmail, String resetLink) {
        sendMail(
                toEmail,
                "[My Project] 비밀번호 변경 링크입니다",
                passwordResetContent(resetLink)
        );
    }

    @Retryable(retryFor = MailSendException.class, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    @Override
    public void sendDormancyWarning(String toEmail, String nickname) {
        sendMail(
                toEmail,
                "[My Project] 계정이 휴면 상태로 전환될 예정입니다",
                dormancyWarningContent(nickname)
        );
    }

    @Recover
    public void recover(MailSendException e, String toEmail, String contentArg) {
        log.error(
                "[SERVICE][EmailService][RETRY_EXHAUSTED] to={} errorType={}",
                toEmail,
                e.getClass().getSimpleName()
        );

        throw e;
    }

    private void sendMail(String to, String title, String content) {
        MimeMessage message = javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(title);
            helper.setText(content, true);
            javaMailSender.send(message);

            log.info("[SERVICE][EmailService][SEND_SUCCESS] to={}", to);
        } catch (MessagingException | MailException e) {
            log.error(
                    "[SERVICE][EmailService][SEND_FAIL] to={} errorType={}",
                    to,
                    e.getClass().getSimpleName()
            );
            throw new MailSendException("email send failed", e);
        }
    }

    private String verificationCodeContent(String code) {
        return String.format(
                """
                <div style="font-family: 'Apple SD Gothic Neo', 'Malgun Gothic', sans-serif; background-color: #f3f4f6; padding: 40px 20px; line-height: 1.6; color: #374151;">
                    <div style="max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);">
                        <div style="background-color: #4f46e5; padding: 24px; text-align: center;">
                            <h2 style="color: #ffffff; margin: 0; font-size: 24px;">이메일 인증</h2>
                        </div>
                        <div style="padding: 40px 32px; text-align: center;">
                            <p style="margin-top: 0; font-size: 16px;">안녕하세요!</p>
                            <p style="font-size: 16px; margin-bottom: 32px;">아래 인증 코드를 입력하여 회원가입을 완료해주세요.</p>
                            
                            <div style="background-color: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 24px; margin-bottom: 32px;">
                                <h1 style="color: #4f46e5; font-size: 36px; margin: 0; letter-spacing: 8px;">%s</h1>
                            </div>
                            
                            <p style="font-size: 14px; color: #6b7280; margin: 0;">이 코드는 <strong>5분간</strong> 유효합니다.</p>
                        </div>
                    </div>
                </div>
                """, code);
    }

    private String passwordResetContent(String resetLink) {
        // %1$s 를 사용하여 resetLink 파라미터 하나로 두 곳(href, text)에 모두 주입합니다.
        return String.format(
                """
                <div style="font-family: 'Apple SD Gothic Neo', 'Malgun Gothic', sans-serif; background-color: #f3f4f6; padding: 40px 20px; line-height: 1.6; color: #374151;">
                    <div style="max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);">
                        <div style="background-color: #4f46e5; padding: 24px; text-align: center;">
                            <h2 style="color: #ffffff; margin: 0; font-size: 24px;">비밀번호 재설정</h2>
                        </div>
                        <div style="padding: 40px 32px; text-align: center;">
                            <p style="margin-top: 0; font-size: 16px;">비밀번호를 잊으셨나요?</p>
                            <p style="font-size: 16px; margin-bottom: 32px;">비밀번호를 재설정하시려면 아래 버튼을 클릭해주세요.</p>
                            
                            <a href="%1$s" style="display: inline-block; background-color: #4f46e5; color: #ffffff; text-decoration: none; padding: 16px 32px; border-radius: 8px; font-weight: bold; font-size: 16px; margin-bottom: 32px;">비밀번호 재설정하기</a>
                            
                            <p style="font-size: 14px; color: #6b7280; margin: 0;">이 링크는 <strong>5분간</strong> 유효합니다.</p>
                            <hr style="border: none; border-top: 1px solid #e5e7eb; margin: 24px 0;">
                            <p style="font-size: 12px; color: #9ca3af; margin: 0; word-break: break-all;">
                                버튼이 작동하지 않는다면 아래 링크를 복사하여 브라우저의 주소창에 붙여넣어 주세요.<br><br>
                                <a href="%1$s" style="color: #4f46e5; text-decoration: underline;">%1$s</a>
                            </p>
                        </div>
                    </div>
                </div>
                """, resetLink);
    }

    private String dormancyWarningContent(String nickname) {
        return String.format(
                """
                <div style="font-family: 'Apple SD Gothic Neo', 'Malgun Gothic', sans-serif; background-color: #f3f4f6; padding: 40px 20px; line-height: 1.6; color: #374151;">
                    <div style="max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);">
                        <div style="padding: 40px 32px; text-align: center;">
                            <div style="margin-bottom: 24px;">
                                <span style="display: inline-block; background-color: #fef2f2; color: #dc2626; padding: 8px 16px; border-radius: 9999px; font-size: 14px; font-weight: bold;">휴면 전환 사전 안내</span>
                            </div>
                            <h2 style="color: #111827; font-size: 20px; margin-bottom: 24px;">안녕하세요, <strong>%s</strong>님.</h2>
                            <p style="font-size: 16px; color: #4b5563; margin-bottom: 8px;">장기간 로그인이 확인되지 않아,</p>
                            <p style="font-size: 16px; color: #4b5563; margin-bottom: 32px;"><strong>1개월 후 계정이 휴면 상태로 전환될 예정</strong>입니다.</p>
                            
                            <div style="background-color: #f9fafb; border-radius: 8px; padding: 20px; text-align: left; margin-bottom: 32px;">
                                <ul style="margin: 0; padding-left: 20px; color: #6b7280; font-size: 14px;">
                                    <li style="margin-bottom: 8px;">휴면 상태로 전환되면 일부 서비스 이용이 제한됩니다.</li>
                                    <li>서비스를 계속 이용하시려면 기간 내에 로그인을 1회 이상 완료해 주세요.</li>
                                </ul>
                            </div>
                        </div>
                        <div style="background-color: #f9fafb; padding: 20px; text-align: center; border-top: 1px solid #e5e7eb;">
                            <p style="margin: 0; font-size: 12px; color: #9ca3af;">본 메일은 발신 전용이며, 회신되지 않습니다.</p>
                        </div>
                    </div>
                </div>
                """, nickname);
    }
}
