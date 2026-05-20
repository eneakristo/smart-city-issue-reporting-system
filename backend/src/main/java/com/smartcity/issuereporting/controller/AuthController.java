package com.smartcity.issuereporting.controller;

import com.smartcity.issuereporting.dto.auth.AuthResponse;
import com.smartcity.issuereporting.dto.auth.ChangePasswordRequest;
import com.smartcity.issuereporting.dto.auth.ForgotPasswordRequest;
import com.smartcity.issuereporting.dto.auth.LoginRequest;
import com.smartcity.issuereporting.dto.auth.RegisterRequest;
import com.smartcity.issuereporting.dto.auth.ResetPasswordRequest;
import com.smartcity.issuereporting.dto.auth.UpdateProfileRequest;
import com.smartcity.issuereporting.dto.auth.UserProfileResponse;
import com.smartcity.issuereporting.dto.response.ApiResponse;
import com.smartcity.issuereporting.security.AuthUser;
import com.smartcity.issuereporting.service.AuthService;
import com.smartcity.issuereporting.service.PasswordResetService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    public AuthController(AuthService authService, PasswordResetService passwordResetService) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestPasswordReset(request.getEmail());
        return ApiResponse.<Void>builder()
                .success(true)
                .message("If an account exists for this email, a reset link has been sent")
                .build();
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.getToken(), request.getPassword());
        return ApiResponse.<Void>builder()
                .success(true)
                .message("Password has been reset")
                .build();
    }

    @GetMapping("/me")
    public UserProfileResponse me(@AuthenticationPrincipal AuthUser authUser) {
        return authService.getCurrentUser(authUser);
    }

    @PutMapping("/me")
    public UserProfileResponse updateProfile(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return authService.updateCurrentUserProfile(authUser, request);
    }

    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        authService.changeCurrentUserPassword(authUser, request);
        return ApiResponse.<Void>builder()
                .success(true)
                .message("Password has been changed")
                .build();
    }
}
