package com.smartcity.issuereporting.service;

import com.smartcity.issuereporting.config.ApplicationProperties;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Service
public class PasswordResetEmailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PasswordResetEmailService.class);

    private final ApplicationProperties applicationProperties;
    private final RestClient restClient;

    public PasswordResetEmailService(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(15));
        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    public void sendPasswordResetEmail(String recipientEmail, String resetUrl) {
        ApplicationProperties.Email emailProperties = applicationProperties.getEmail();
        if (!hasBrevoConfiguration(emailProperties)) {
            LOGGER.warn(
                    "Email API is not configured. provider={}, hasApiKey={}, hasFromAddress={}, hasFromName={}, hasBaseUrl={}. Password reset link for {}: {}",
                    emailProperties.getProvider(),
                    StringUtils.hasText(emailProperties.getBrevoApiKey()),
                    StringUtils.hasText(emailProperties.getFromAddress()),
                    StringUtils.hasText(emailProperties.getFromName()),
                    StringUtils.hasText(emailProperties.getBrevoBaseUrl()),
                    recipientEmail,
                    resetUrl
            );
            return;
        }

        BrevoEmailRequest request = new BrevoEmailRequest(
                new BrevoSender(emailProperties.getFromName(), emailProperties.getFromAddress()),
                List.of(new BrevoRecipient(recipientEmail)),
                "Reset your Smart City password",
                buildHtmlBody(resetUrl),
                buildPlainTextBody(resetUrl)
        );

        restClient.post()
                .uri(URI.create(trimTrailingSlash(emailProperties.getBrevoBaseUrl()) + "/smtp/email"))
                .header("api-key", emailProperties.getBrevoApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::handleEmailApiFailure)
                .toBodilessEntity();

        LOGGER.info("Password reset email API accepted request for recipient={}", recipientEmail);
    }

    private String buildPlainTextBody(String resetUrl) {
        return """
                We received a request to reset your Smart City Issue Reporting password.

                Use this link to set a new password:
                %s

                This link expires in %d minutes. If you did not request this, you can ignore this email.
                """.formatted(resetUrl, applicationProperties.getPasswordReset().getTokenExpirationMinutes());
    }

    private String buildHtmlBody(String resetUrl) {
        return """
                <p>We received a request to reset your Smart City Issue Reporting password.</p>
                <p>Use this link to set a new password:</p>
                <p><a href="%s">%s</a></p>
                <p>This link expires in %d minutes. If you did not request this, you can ignore this email.</p>
                """.formatted(resetUrl, resetUrl, applicationProperties.getPasswordReset().getTokenExpirationMinutes());
    }

    private boolean hasBrevoConfiguration(ApplicationProperties.Email emailProperties) {
        return "brevo".equalsIgnoreCase(emailProperties.getProvider())
                && StringUtils.hasText(emailProperties.getBrevoApiKey())
                && StringUtils.hasText(emailProperties.getFromAddress())
                && StringUtils.hasText(emailProperties.getFromName())
                && StringUtils.hasText(emailProperties.getBrevoBaseUrl());
    }

    private void handleEmailApiFailure(org.springframework.http.HttpRequest request, ClientHttpResponse response)
            throws IOException {
        String responseBody = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8).trim();
        LOGGER.error("Password reset email API failed. status={}, body={}", response.getStatusCode().value(), responseBody);

        String message = responseBody.isBlank()
                ? "Password reset email API failed with status " + response.getStatusCode().value()
                : "Password reset email API failed with status " + response.getStatusCode().value() + ": " + responseBody;
        throw new IllegalStateException(message);
    }

    private String trimTrailingSlash(String value) {
        return value != null && value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private record BrevoEmailRequest(
            BrevoSender sender,
            List<BrevoRecipient> to,
            String subject,
            String htmlContent,
            String textContent
    ) {
    }

    private record BrevoSender(
            String name,
            String email
    ) {
    }

    private record BrevoRecipient(
            String email
    ) {
    }
}
