package com.example.my_project_1.auth.service.impl;

import com.example.my_project_1.auth.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender javaMailSender;

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

    private void sendMail(String to, String title, String content) {
        MimeMessage message = javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(title);
            helper.setText(content, true);
            javaMailSender.send(message);
            log.info("Email sent successfully to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send email", e);
            throw new RuntimeException("이메일 발송에 실패했습니다.");
        }
    }
}
