package com.smartcity.issuereporting.service;

import com.smartcity.issuereporting.config.ApplicationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class PasswordResetEmailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PasswordResetEmailService.class);

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;
    private final ApplicationProperties applicationProperties;

    public PasswordResetEmailService(
            ObjectProvider<JavaMailSender> mailSender,
            MailProperties mailProperties,
            ApplicationProperties applicationProperties
    ) {
        this.mailSender = mailSender.getIfAvailable();
        this.mailProperties = mailProperties;
        this.applicationProperties = applicationProperties;
    }

    public void sendPasswordResetEmail(String recipientEmail, String resetUrl) {
        if (!hasSmtpCredentials()) {
            LOGGER.warn("SMTP is not configured. Password reset link for {}: {}", recipientEmail, resetUrl);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(recipientEmail);
        message.setFrom(mailProperties.getUsername());
        message.setSubject("Reset your Smart City password");
        message.setText("""
                We received a request to reset your Smart City Issue Reporting password.

                Use this link to set a new password:
                %s

                This link expires in %d minutes. If you did not request this, you can ignore this email.
                """.formatted(resetUrl, applicationProperties.getPasswordReset().getTokenExpirationMinutes()));

        mailSender.send(message);
    }

    private boolean hasSmtpCredentials() {
        return mailSender != null
                && mailProperties.getHost() != null
                && !mailProperties.getHost().isBlank()
                && mailProperties.getUsername() != null
                && !mailProperties.getUsername().isBlank()
                && mailProperties.getPassword() != null
                && !mailProperties.getPassword().isBlank();
    }
}
