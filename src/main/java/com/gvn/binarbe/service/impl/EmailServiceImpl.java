package com.gvn.binarbe.service.impl;

import com.gvn.binarbe.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Implementation of EmailService for sending emails via SMTP.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.password-reset.base-url}")
    private String resetBaseUrl;

    @Override
    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        log.info("Sending password reset email to: {}", toEmail);

        String resetLink = resetBaseUrl + "?token=" + resetToken;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("noreply@binar-be.com");
        message.setTo(toEmail);
        message.setSubject("Password Reset Request");
        message.setText(buildEmailBody(resetLink));

        mailSender.send(message);

        log.info("Password reset email sent successfully to: {}", toEmail);
    }

    private String buildEmailBody(String resetLink) {
        return """
                Hello,

                You have requested to reset your password.

                Please click the link below to reset your password:
                %s

                This link will expire in 30 minutes.

                If you did not request this password reset, please ignore this email.

                Best regards,
                PENPENPEN - EHEFIN
                """.formatted(resetLink);
    }
}
