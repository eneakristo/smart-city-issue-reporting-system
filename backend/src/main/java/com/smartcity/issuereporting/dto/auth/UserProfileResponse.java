package com.smartcity.issuereporting.dto.auth;

import com.smartcity.issuereporting.model.entity.Role;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserProfileResponse {

    private final UUID id;
    private final String fullName;
    private final String email;
    private final String phoneNumber;
    private final String bio;
    private final String profilePictureUrl;
    private final Role role;
}
