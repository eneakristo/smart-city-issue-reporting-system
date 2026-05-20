package com.smartcity.issuereporting.service;

import com.smartcity.issuereporting.dto.auth.AuthResponse;
import com.smartcity.issuereporting.dto.auth.ChangePasswordRequest;
import com.smartcity.issuereporting.dto.auth.LoginRequest;
import com.smartcity.issuereporting.dto.auth.RegisterRequest;
import com.smartcity.issuereporting.dto.auth.UpdateProfileRequest;
import com.smartcity.issuereporting.dto.auth.UserProfileResponse;
import com.smartcity.issuereporting.exception.AuthenticationException;
import com.smartcity.issuereporting.exception.EmailAlreadyExistsException;
import com.smartcity.issuereporting.model.entity.Role;
import com.smartcity.issuereporting.model.entity.User;
import com.smartcity.issuereporting.repository.UserRepository;
import com.smartcity.issuereporting.security.AuthUser;
import com.smartcity.issuereporting.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new EmailAlreadyExistsException("An account with this email already exists");
        }

        User user = new User();
        user.setFullName(request.getFullName().trim());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.CITIZEN);

        User savedUser = userRepository.save(user);
        AuthUser authUser = toAuthUser(savedUser);

        return buildAuthResponse(savedUser, jwtService.generateAccessToken(authUser));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new AuthenticationException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new AuthenticationException("Invalid email or password");
        }

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new AuthenticationException("This account has been removed");
        }

        AuthUser authUser = toAuthUser(user);
        return buildAuthResponse(user, jwtService.generateAccessToken(authUser));
    }

    public UserProfileResponse getCurrentUser(AuthUser authUser) {
        User user = userRepository.findById(authUser.getId())
                .orElseThrow(() -> new AuthenticationException("Authenticated user not found"));

        return UserProfileResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .bio(user.getBio())
                .profilePictureUrl(user.getProfilePictureUrl())
                .role(user.getRole())
                .build();
    }

    @Transactional
    public UserProfileResponse updateCurrentUserProfile(AuthUser authUser, UpdateProfileRequest request) {
        User user = userRepository.findById(authUser.getId())
                .orElseThrow(() -> new AuthenticationException("Authenticated user not found"));

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new AuthenticationException("This account has been removed");
        }

        user.setFullName(request.getFullName().trim());
        user.setPhoneNumber(normalizeOptionalText(request.getPhoneNumber()));
        user.setBio(normalizeOptionalText(request.getBio()));
        user.setProfilePictureUrl(normalizeOptionalText(request.getProfilePictureUrl()));

        return toProfileResponse(userRepository.save(user));
    }

    @Transactional
    public void changeCurrentUserPassword(AuthUser authUser, ChangePasswordRequest request) {
        User user = userRepository.findById(authUser.getId())
                .orElseThrow(() -> new AuthenticationException("Authenticated user not found"));

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new AuthenticationException("This account has been removed");
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new AuthenticationException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    private AuthResponse buildAuthResponse(User user, String accessToken) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .bio(user.getBio())
                .profilePictureUrl(user.getProfilePictureUrl())
                .role(user.getRole())
                .build();
    }

    private UserProfileResponse toProfileResponse(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .bio(user.getBio())
                .profilePictureUrl(user.getProfilePictureUrl())
                .role(user.getRole())
                .build();
    }

    private String normalizeOptionalText(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return value.trim();
    }

    private AuthUser toAuthUser(User user) {
        return new AuthUser(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getRole(),
                Boolean.TRUE.equals(user.getActive())
        );
    }
}
