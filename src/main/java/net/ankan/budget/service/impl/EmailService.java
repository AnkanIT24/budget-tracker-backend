
package net.ankan.budget.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:}")
    private String from;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${app.reset.expiry-minutes:30}")
    private long expiryMinutes;

    @Async
    public void sendPasswordResetEmail(
            String to,
            String fullName,
            String rawToken
    ) {
        String link = frontendUrl + "/reset-password?token=" + rawToken;

        String body = """
                Hi %s,

                We received a request to reset your BudgetTracker password.

                Open this link to choose a new one:
                %s

                Using the mobile app instead? Paste this code into the
                "Reset password" screen:

                %s

                This link expires in %d minutes and can only be used once.
                If you didn't request a reset, you can ignore this email.
                Your password stays unchanged.

                BudgetTracker
                """.formatted(fullName, link, rawToken, expiryMinutes);

        try {
            SimpleMailMessage message = new SimpleMailMessage();

            if (from != null && !from.isBlank()) {
                message.setFrom(from);
            }

            message.setTo(to);
            message.setSubject("Reset your BudgetTracker password");
            message.setText(body);

            mailSender.send(message);

            log.info(
                    "Password reset email dispatched to {}",
                    maskEmail(to)
            );

        } catch (Exception ex) {
            log.error(
                    "Failed to send password reset email to {}: {}",
                    maskEmail(to),
                    ex.getMessage(),
                    ex
            );
        }
    }

    private String maskEmail(String email) {
        int at = email.indexOf('@');

        if (at <= 1) {
            return "***";
        }

        return email.charAt(0) + "***" + email.substring(at);
    }
}
