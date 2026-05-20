package com.smartcity.issuereporting.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProfileRequest {

    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Full name must be at most 100 characters")
    private String fullName;

    @Size(max = 30, message = "Phone number must be at most 30 characters")
    private String phoneNumber;

    @Size(max = 600, message = "Bio must be at most 600 characters")
    private String bio;

    @Size(max = 1_500_000, message = "Profile picture is too large")
    private String profilePictureUrl;
}
