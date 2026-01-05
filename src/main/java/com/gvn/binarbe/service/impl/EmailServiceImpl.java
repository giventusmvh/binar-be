package com.gvn.binarbe.service.impl;

import com.gvn.binarbe.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/** Implementation of EmailService for sending emails via SMTP. */
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

    try {
      MimeMessage mimeMessage = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

      helper.setFrom("noreply@ehefin.com");
      helper.setTo(toEmail);
      helper.setSubject("🔐 Password Reset Request");
      helper.setText(buildHtmlEmailBody(resetLink, resetToken), true);

      mailSender.send(mimeMessage);

      log.info("Password reset email sent successfully to: {}", toEmail);
    } catch (MessagingException e) {
      log.error("Failed to send password reset email to: {}", toEmail, e);
      throw new RuntimeException("Failed to send password reset email", e);
    }
  }

  private String buildHtmlEmailBody(String resetLink, String token) {
    return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                </head>
                <body style="margin: 0; padding: 0; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f4f4;">
                    <table width="100%%" cellpadding="0" cellspacing="0" style="background-color: #f4f4f4; padding: 40px 20px;">
                        <tr>
                            <td align="center">
                                <table width="600" cellpadding="0" cellspacing="0" style="background-color: #ffffff; border-radius: 12px; box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);">
                                    <!-- Header -->
                                    <tr>
                                        <td style="background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); padding: 40px 40px; border-radius: 12px 12px 0 0; text-align: center;">
                                            <h1 style="color: #ffffff; margin: 0; font-size: 28px; font-weight: 600;">🔐 Password Reset</h1>
                                            <p style="color: rgba(255,255,255,0.9); margin: 10px 0 0 0; font-size: 14px;">EHEFIN - Secure Banking Platform</p>
                                        </td>
                                    </tr>

                                    <!-- Content -->
                                    <tr>
                                        <td style="padding: 40px;">
                                            <p style="color: #333333; font-size: 16px; line-height: 1.6; margin: 0 0 20px 0;">
                                                Hello,
                                            </p>
                                            <p style="color: #333333; font-size: 16px; line-height: 1.6; margin: 0 0 20px 0;">
                                                We received a request to reset your password. Click the button below to create a new password:
                                            </p>

                                            <!-- Button -->
                                            <table width="100%%" cellpadding="0" cellspacing="0" style="margin: 30px 0;">
                                                <tr>
                                                    <td align="center">
                                                        <a href="%s" style="display: inline-block; background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: #ffffff; text-decoration: none; padding: 16px 40px; border-radius: 8px; font-size: 16px; font-weight: 600; box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);">
                                                            Reset Password
                                                        </a>
                                                    </td>
                                                </tr>
                                            </table>

                                            <!-- Divider -->
                                            <hr style="border: none; border-top: 1px solid #e0e0e0; margin: 30px 0;">

                                            <!-- Copy Link Section -->
                                            <p style="color: #666666; font-size: 14px; margin: 0 0 10px 0;">
                                                Or copy and paste this link into your browser:
                                            </p>
                                            <div style="background-color: #f8f9fa; border: 1px solid #e0e0e0; border-radius: 6px; padding: 12px; word-break: break-all;">
                                                <code style="color: #667eea; font-size: 13px;">%s</code>
                                            </div>

                                            <!-- Token Section -->
                                            <p style="color: #666666; font-size: 14px; margin: 20px 0 10px 0;">
                                                Your reset token (if needed):
                                            </p>
                                            <div style="background-color: #fff3cd; border: 1px solid #ffc107; border-radius: 6px; padding: 12px; text-align: center;">
                                                <code style="color: #856404; font-size: 14px; font-weight: 600; letter-spacing: 1px;">%s</code>
                                            </div>

                                            <!-- Warning -->
                                            <div style="background-color: #fff5f5; border-left: 4px solid #e53e3e; padding: 15px; margin: 30px 0; border-radius: 0 6px 6px 0;">
                                                <p style="color: #c53030; font-size: 14px; margin: 0;">
                                                    ⚠️ This link will expire in <strong>30 minutes</strong>.
                                                </p>
                                            </div>

                                            <p style="color: #666666; font-size: 14px; line-height: 1.6; margin: 0;">
                                                If you didn't request this password reset, you can safely ignore this email. Your password will remain unchanged.
                                            </p>
                                        </td>
                                    </tr>

                                    <!-- Footer -->
                                    <tr>
                                        <td style="background-color: #f8f9fa; padding: 25px 40px; border-radius: 0 0 12px 12px; text-align: center; border-top: 1px solid #e0e0e0;">
                                            <p style="color: #999999; font-size: 12px; margin: 0 0 5px 0;">
                                                © 2024 EHEFIN. All rights reserved.
                                            </p>
                                            <p style="color: #999999; font-size: 12px; margin: 0;">
                                                This is an automated message, please do not reply.
                                            </p>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """
        .formatted(resetLink, resetLink, token);
  }
}
