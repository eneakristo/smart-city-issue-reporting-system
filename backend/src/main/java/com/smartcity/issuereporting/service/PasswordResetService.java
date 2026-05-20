package com.smartcity.issuereporting.service;

import com.smartcity.issuereporting.config.ApplicationProperties;
import com.smartcity.issuereporting.exception.BadRequestException;
import com.smartcity.issuereporting.model.entity.User;
import com.smartcity.issuereporting.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class PasswordResetService {

    private final Map<String, PasswordResetToken> resetTokens = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetEmailService emailService;
    private final ApplicationProperties applicationProperties;

    public PasswordResetService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            PasswordResetEmailService emailService,
            ApplicationProperties applicationProperties
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.applicationProperties = applicationProperties;
    }

    @Transactional(readOnly = true)
    public void requestPasswordReset(String email) {
        String normalizedEmail = email.trim().toLowerCase();

        userRepository.findByEmailIgnoreCase(normalizedEmail)
                .filter(user -> Boolean.TRUE.equals(user.getActive()))
                .ifPresent(user -> {
                    String token = generateToken();
                    String tokenHash = hashToken(token);
                    Instant expiresAt = Instant.now().plusSeconds(
                            applicationProperties.getPasswordReset().getTokenExpirationMinutes() * 60L
                    );

                    resetTokens.put(tokenHash, new PasswordResetToken(user.getId(), expiresAt));
                    emailService.sendPasswordResetEmail(user.getEmail(), buildResetUrl(token));
                });
    }

    @Transactional
    public void resetPassword(String token, String password) {
        String tokenHash = hashToken(token);
        PasswordResetToken resetToken = resetTokens.remove(tokenHash);

        if (resetToken == null || resetToken.expiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("Reset link is invalid or expired");
        }

        User user = userRepository.findById(resetToken.userId())
                .orElseThrow(() -> new BadRequestException("Reset link is invalid or expired"));

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new BadRequestException("This account has been removed");
        }

        user.setPasswordHash(passwordEncoder.encode(password));
        userRepository.save(user);
    }

    private String buildResetUrl(String token) {
        return UriComponentsBuilder
                .fromUriString(applicationProperties.getPasswordReset().getFrontendResetUrl())
                .queryParam("token", token)
                .build()
                .toUriString();
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private record PasswordResetToken(UUID userId, Instant expiresAt) {
    }
}
